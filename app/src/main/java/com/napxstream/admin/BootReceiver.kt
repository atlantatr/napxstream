package com.napxstream.admin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.napxstream.XtreamApp

/**
 * Cihaz yeniden başladığında (özellikle her zaman açık duran Android TV box'larda)
 * uygulamayı hiç elle açmaya gerek kalmadan yönetim panelini otomatik başlatır.
 * Yalnızca kullanıcı paneli devre dışı bırakmadıysa (varsayılan: etkin) çalışır.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as XtreamApp
        if (!app.prefsManager.isAdminServerEnabled()) return

        AdminServerService.start(context)
    }
}
