package com.napxstream.admin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.napxstream.R
import com.napxstream.XtreamApp
import com.napxstream.ui.main.MainActivity
import java.net.NetworkInterface
import java.util.Collections

/**
 * Yönetim paneli HTTP sunucusunu (AdminHttpServer) bir ön plan (foreground) servis
 * içinde çalıştırır — Android arka plan kısıtlamaları nedeniyle uygulama arka plandayken
 * de sunucunun yanıt vermeye devam etmesi için bu gereklidir. Bildirim, kullanıcının
 * sunucunun çalıştığını her zaman görmesini sağlar (gizli/sessiz bir arka kapı değildir).
 *
 * ÖNEMLİ: startForeground() bazı Android sürümlerinde/OEM'lerde (özellikle arka planda
 * tetiklenen başlatmalarda) istisna fırlatabilir (ForegroundServiceStartNotAllowedException
 * vb.). Bu istisna yakalanmazsa servis - ve dolayısıyla tüm uygulama süreci - her açılışta
 * aynı noktada çöker (crash loop). Bu yüzden TÜM başlatma akışı try/catch ile korunur;
 * başarısız olursa panel sadece devre dışı kalır, uygulama asla çökmez.
 */
class AdminServerService : Service() {

    private var server: AdminHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
        } catch (e: Exception) {
            Log.w(TAG, "Bildirim kanalı oluşturulamadı: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            startServerAndForeground()
            START_STICKY
        } catch (e: Exception) {
            // Sunucu veya bildirim başlatılamadı — uygulamayı çökertmek yerine sessizce
            // servisi durduruyoruz. Panel bu oturumda kullanılamaz ama uygulama normal
            // çalışmaya devam eder.
            Log.w(TAG, "Yönetim paneli başlatılamadı: ${e.message}")
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun startServerAndForeground() {
        val app = application as XtreamApp
        val port = app.prefsManager.getAdminPort()

        // Android, startForegroundService() çağrısından sonra startForeground()'un birkaç
        // saniye içinde çağrılmasını zorunlu kılar; bu yüzden bildirimi HEMEN gösteriyoruz.
        // Asıl HTTP sunucusunu (birkaç kısa yeniden deneme ile) ayrı bir thread'de
        // başlatıyoruz ki hem bu kural asla ihlal edilmesin hem de ana thread bloklanmasın —
        // kullanıcının panele erişmek için "bir süre beklemesi" gereken gecikme buradan
        // kaynaklanıyordu.
        startForeground(NOTIFICATION_ID, buildNotification(port))

        Thread {
            server?.stop()
            server = null
            for (attempt in 1..5) {
                try {
                    server = AdminHttpServer(applicationContext, port).apply { start(SOCKET_READ_TIMEOUT, false) }
                    return@Thread
                } catch (e: Exception) {
                    Log.w(TAG, "Sunucu başlatma denemesi $attempt başarısız: ${e.message}")
                    Thread.sleep(150L * attempt)
                }
            }
            Log.e(TAG, "Yönetim paneli sunucusu birden fazla denemeye rağmen başlatılamadı")
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            server?.stop()
        } catch (e: Exception) {
            // yoksay
        }
        server = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(port: Int): Notification {
        val ip = getLocalIpAddress() ?: "cihaz-ip-adresi"
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.admin_notification_title))
            .setContentText("http://$ip:$port")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Yönetim Paneli", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Yerel ağ yönetim paneli sunucusu çalışırken gösterilir" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "AdminServerService"
        private const val CHANNEL_ID = "admin_server_channel"
        private const val NOTIFICATION_ID = 4201
        private const val SOCKET_READ_TIMEOUT = 30000

        /** Cihazın yerel Wi-Fi IP adresini bulur (kullanıcıya "şu adrese bağlan" demek için). */
        fun getLocalIpAddress(): String? {
            return try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addresses = Collections.list(intf.inetAddresses)
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                            return addr.hostAddress
                        }
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }

        fun start(context: Context) {
            try {
                val intent = Intent(context, AdminServerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Bazı Android sürümleri/OEM'ler belirli bağlamlardan (ör. BOOT_COMPLETED,
                // Application.onCreate) ön plan servisi başlatmayı reddedebilir. Bu durumda
                // sessizce vazgeçiyoruz — uygulamanın geri kalanı etkilenmez.
                Log.w(TAG, "AdminServerService başlatılamadı: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, AdminServerService::class.java))
            } catch (e: Exception) {
                // yoksay
            }
        }
    }
}
