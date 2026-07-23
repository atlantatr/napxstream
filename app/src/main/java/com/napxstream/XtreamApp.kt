package com.napxstream

import android.app.Application
import com.napxstream.admin.AdminServerService
import com.napxstream.data.local.AppDatabase
import com.napxstream.data.repository.AccountRepository
import com.napxstream.data.repository.M3uRepository
import com.napxstream.data.repository.TmdbRepository
import com.napxstream.data.repository.XtreamRepository
import com.napxstream.util.PrefsManager

class XtreamApp : Application() {

    lateinit var repository: XtreamRepository
        private set

    lateinit var tmdbRepository: TmdbRepository
        private set

    lateinit var m3uRepository: M3uRepository
        private set

    lateinit var accountRepository: AccountRepository
        private set

    lateinit var prefsManager: PrefsManager
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        prefsManager = PrefsManager(this)
        repository = XtreamRepository(db)
        tmdbRepository = TmdbRepository(db)
        m3uRepository = M3uRepository(db)
        accountRepository = AccountRepository(db.accountDao(), prefsManager)

        autoStartAdminServerIfNeeded()
    }

    /**
     * Yönetim paneli varsayılan olarak etkindir (bkz. PrefsManager.isAdminServerEnabled).
     * Kullanıcı hiç Ayarlar'a girmemiş olsa bile, uygulama her açıldığında (veya cihaz
     * yeniden başladığında, bkz. BootReceiver) sunucu otomatik çalışır. Panel şifresi
     * opsiyoneldir; kullanıcı Ayarlar > Uzaktan Yönetim Paneli'nden isterse ekler.
     */
    private fun autoStartAdminServerIfNeeded() {
        if (!prefsManager.isAdminServerEnabled()) return
        AdminServerService.start(this)
    }
}
