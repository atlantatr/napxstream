package com.napxstream.data.repository

import com.napxstream.data.api.XtreamAccount
import com.napxstream.data.local.AccountDao
import com.napxstream.data.local.AccountEntity
import com.napxstream.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cihazda kayıtlı tüm hesapları (Xtream/M3U) yönetir. Aktif hesap her zaman
 * PrefsManager'daki (şifreli) alanlarla senkron tutulur — böylece mevcut tüm
 * ekranlar (LiveFragment, VodFragment vb.) değişmeden `prefsManager.getAccount()`
 * ile çalışmaya devam eder; bu repository sadece "hangi hesap aktif" ve
 * "kayıtlı hesap listesi" sorumluluğunu üstlenir. Yerel yönetim paneli (AdminHttpServer)
 * bu sınıf üzerinden hesap ekler/siler/değiştirir.
 */
class AccountRepository(
    private val dao: AccountDao,
    private val prefsManager: PrefsManager
) {

    fun getAllLive() = dao.getAllLive()

    suspend fun getAll(): List<AccountEntity> = withContext(Dispatchers.IO) { dao.getAll() }

    suspend fun getActive(): AccountEntity? = withContext(Dispatchers.IO) { dao.getActive() }

    /** Yeni bir Xtream hesabı ekler; ilk hesapsa otomatik aktif yapılır. */
    suspend fun addXtreamAccount(
        label: String, host: String, port: String, username: String, password: String, useHttps: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val makeActive = dao.getAll().isEmpty()
        val id = dao.insert(
            AccountEntity(
                label = label, type = PrefsManager.SOURCE_XTREAM,
                host = host, port = port, username = username, password = password, useHttps = useHttps,
                isActive = makeActive
            )
        )
        if (makeActive) activate(id)
        id
    }

    /** Yeni bir M3U hesabı ekler; ilk hesapsa otomatik aktif yapılır. */
    suspend fun addM3uAccount(label: String, m3uUrl: String, epgUrl: String?): Long = withContext(Dispatchers.IO) {
        val makeActive = dao.getAll().isEmpty()
        val id = dao.insert(
            AccountEntity(
                label = label, type = PrefsManager.SOURCE_M3U,
                m3uUrl = m3uUrl, m3uEpgUrl = epgUrl,
                isActive = makeActive
            )
        )
        if (makeActive) activate(id)
        id
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val wasActive = dao.getById(id)?.isActive == true
        dao.delete(id)
        if (wasActive) {
            // Aktif hesap silindiyse kalan ilk hesabı (varsa) aktif yap
            val remaining = dao.getAll().firstOrNull()
            if (remaining != null) activate(remaining.id) else prefsManager.clearAccount()
        }
    }

    /** Belirtilen hesabı aktif yapar ve PrefsManager'daki (şifreli, hızlı erişim) kopyayı günceller. */
    suspend fun activate(id: Long) = withContext(Dispatchers.IO) {
        val account = dao.getById(id) ?: return@withContext
        dao.deactivateAll()
        dao.activate(id)

        if (account.type == PrefsManager.SOURCE_XTREAM) {
            prefsManager.saveAccount(
                XtreamAccount(
                    host = account.host ?: "", port = account.port ?: "",
                    username = account.username ?: "", password = account.password ?: "",
                    useHttps = account.useHttps
                )
            )
        } else {
            prefsManager.saveM3uSource(account.m3uUrl ?: "", account.m3uEpgUrl)
        }
    }

    /**
     * Uygulama ilk kez giriş ekranından (yönetim paneli değil, normal Login akışı) hesap
     * kaydettiğinde bu hesabı da Room tablosuna yazar ki panelde görünsün.
     */
    suspend fun syncFromPrefsIfMissing(label: String = "Varsayılan Hesap") = withContext(Dispatchers.IO) {
        if (dao.getActive() != null) return@withContext
        val account = prefsManager.getAccount()
        if (account != null) {
            addXtreamAccount(label, account.host, account.port, account.username, account.password, account.useHttps)
            return@withContext
        }
        val m3uUrl = prefsManager.getM3uUrl()
        if (!m3uUrl.isNullOrBlank()) {
            addM3uAccount(label, m3uUrl, prefsManager.getM3uEpgUrl())
        }
    }
}
