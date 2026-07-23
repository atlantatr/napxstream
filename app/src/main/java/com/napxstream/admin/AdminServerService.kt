package com.napxstream.admin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
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
 */
class AdminServerService : Service() {

    private var server: AdminHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as XtreamApp
        val port = app.prefsManager.getAdminPort()
        // Şifre opsiyoneldir — kullanıcı Ayarlar'dan isterse belirler. Boşsa panel
        // yerel ağda şifresiz erişilebilir olur (bkz. AdminHttpServer.serve).

        try {
            server?.stop()
            server = AdminHttpServer(applicationContext, port).apply { start(SOCKET_READ_TIMEOUT, false) }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(port))
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
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
            val intent = Intent(context, AdminServerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AdminServerService::class.java))
        }
    }
}
