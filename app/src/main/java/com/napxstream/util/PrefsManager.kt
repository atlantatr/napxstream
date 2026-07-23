package com.napxstream.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.napxstream.data.api.XtreamAccount

/**
 * Kullanıcının hesap bilgilerini (Xtream veya M3U), TMDB anahtarını ve ebeveyn
 * kilidi ayarlarını cihazda ŞİFRELİ olarak saklar (AndroidX Security /
 * EncryptedSharedPreferences). Anahtar Android Keystore'da tutulur; dosya
 * diskte açık okunamaz.
 */
class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Keystore erişilemezse (ör. bazı eski/özel TV kutuları) düz SharedPreferences'a düş.
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ---------------- Hesap (Xtream Codes) ----------------
    fun saveAccount(account: XtreamAccount) {
        prefs.edit()
            .putString(KEY_HOST, account.host)
            .putString(KEY_PORT, account.port)
            .putString(KEY_USERNAME, account.username)
            .putString(KEY_PASSWORD, account.password)
            .putBoolean(KEY_HTTPS, account.useHttps)
            .putString(KEY_SOURCE_TYPE, SOURCE_XTREAM)
            .apply()
    }

    fun getAccount(): XtreamAccount? {
        if (isM3uSource()) return null
        val host = prefs.getString(KEY_HOST, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        val port = prefs.getString(KEY_PORT, "") ?: ""
        val https = prefs.getBoolean(KEY_HTTPS, false)
        return XtreamAccount(host, port, username, password, https)
    }

    /** Hem Xtream hem M3U kaynağını (ve kaynak tipi işaretini) temizler; tam çıkış içindir. */
    fun clearAccount() {
        prefs.edit()
            .remove(KEY_HOST).remove(KEY_PORT).remove(KEY_USERNAME)
            .remove(KEY_PASSWORD).remove(KEY_HTTPS)
            .remove(KEY_M3U_URL).remove(KEY_M3U_EPG_URL)
            .remove(KEY_SOURCE_TYPE)
            .apply()
        // Not: Ebeveyn kilidi PIN'i ve TMDB anahtarı bilinçli olarak silinmiyor;
        // hesap/kaynak değişse de bu ayarlar kalıcı.
    }

    fun isLoggedIn(): Boolean = getAccount() != null || !getM3uUrl().isNullOrBlank()

    // ---------------- Ebeveyn Kilidi ----------------
    fun isParentalLockEnabled(): Boolean = prefs.getBoolean(KEY_PARENTAL_ENABLED, false)

    fun setParentalLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PARENTAL_ENABLED, enabled).apply()
    }

    fun hasParentalPin(): Boolean = !prefs.getString(KEY_PARENTAL_PIN, null).isNullOrBlank()

    fun setParentalPin(pin: String) {
        prefs.edit().putString(KEY_PARENTAL_PIN, pin).apply()
    }

    fun verifyParentalPin(pin: String): Boolean = prefs.getString(KEY_PARENTAL_PIN, null) == pin

    fun clearParentalPin() {
        prefs.edit().remove(KEY_PARENTAL_PIN).putBoolean(KEY_PARENTAL_ENABLED, false).apply()
    }

    // ---------------- TMDB Zenginleştirme ----------------
    fun getTmdbApiKey(): String? = prefs.getString(KEY_TMDB_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun setTmdbApiKey(key: String) {
        prefs.edit().putString(KEY_TMDB_API_KEY, key.trim()).apply()
    }

    fun clearTmdbApiKey() {
        prefs.edit().remove(KEY_TMDB_API_KEY).apply()
    }

    fun isTmdbEnrichmentEnabled(): Boolean = !getTmdbApiKey().isNullOrBlank()

    // ---------------- Kaynak Tipi (Xtream / M3U) ----------------
    /** "xtream" | "m3u" — hesap kaydedilirken otomatik ayarlanır */
    fun getSourceType(): String = prefs.getString(KEY_SOURCE_TYPE, SOURCE_XTREAM) ?: SOURCE_XTREAM

    fun isM3uSource(): Boolean = getSourceType() == SOURCE_M3U

    fun saveM3uSource(playlistUrl: String, epgUrl: String?) {
        prefs.edit()
            .putString(KEY_SOURCE_TYPE, SOURCE_M3U)
            .putString(KEY_M3U_URL, playlistUrl.trim())
            .putString(KEY_M3U_EPG_URL, epgUrl?.trim())
            .apply()
    }

    fun getM3uUrl(): String? = prefs.getString(KEY_M3U_URL, null)

    fun getM3uEpgUrl(): String? = prefs.getString(KEY_M3U_EPG_URL, null)

    // ---------------- Uzaktan Yönetim Paneli (yerel ağ üzerinden) ----------------
    fun isAdminServerEnabled(): Boolean = prefs.getBoolean(KEY_ADMIN_ENABLED, true)

    fun setAdminServerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADMIN_ENABLED, enabled).apply()
    }

    fun getAdminPort(): Int = prefs.getInt(KEY_ADMIN_PORT, 8090)

    fun setAdminPort(port: Int) {
        prefs.edit().putInt(KEY_ADMIN_PORT, port).apply()
    }

    /** Panelin kendi giriş şifresi — hesap şifrelerinden bağımsız, ayrı bir korumadır. */
    fun getAdminPassword(): String? = prefs.getString(KEY_ADMIN_PASSWORD, null)?.takeIf { it.isNotBlank() }

    fun setAdminPassword(password: String) {
        prefs.edit().putString(KEY_ADMIN_PASSWORD, password).apply()
    }

    fun clearAdminPassword() {
        prefs.edit().remove(KEY_ADMIN_PASSWORD).apply()
    }

    fun hasAdminPassword(): Boolean = !getAdminPassword().isNullOrBlank()

    companion object {
        private const val PREFS_NAME = "napxstream_secure_prefs"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_HTTPS = "https"
        private const val KEY_PARENTAL_ENABLED = "parental_lock_enabled"
        private const val KEY_PARENTAL_PIN = "parental_pin"
        private const val KEY_TMDB_API_KEY = "tmdb_api_key"
        private const val KEY_SOURCE_TYPE = "source_type"
        private const val KEY_M3U_URL = "m3u_url"
        private const val KEY_M3U_EPG_URL = "m3u_epg_url"
        private const val KEY_ADMIN_ENABLED = "admin_server_enabled"
        private const val KEY_ADMIN_PORT = "admin_server_port"
        private const val KEY_ADMIN_PASSWORD = "admin_server_password"
        const val SOURCE_XTREAM = "xtream"
        const val SOURCE_M3U = "m3u"
    }
}
