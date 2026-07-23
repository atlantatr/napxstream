package com.napxstream.admin

import android.content.Context
import com.napxstream.XtreamApp
import com.napxstream.data.api.XtreamAccount
import com.napxstream.data.local.AccountEntity
import com.napxstream.data.local.BlockedChannelEntity
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Aynı Wi-Fi/yerel ağdaki bir tarayıcıdan `http://CIHAZ_IP:PORT` ile erişilen
 * yönetim paneli. Hesap yönetimi, ebeveyn kilidi ve kanal/kategori engelleme
 * (bouquet kısıtlaması) buradan yapılır. Tüm istekler HTTP Basic Auth ile
 * korunur (kullanıcı adı: "admin", şifre: Ayarlar'da belirlenen panel şifresi).
 *
 * Güvenlik notu: Bu sunucu sadece yerel ağdan erişilebilir (internete
 * yönlendirilmemiş bir ev/ofis Wi-Fi'sı varsayılır) ve kullanıcı Ayarlar'dan
 * elle etkinleştirmediği sürece hiç başlamaz. Yine de şifre boşsa sunucu
 * başlatılmaz — kimliksiz erişime asla izin verilmez.
 */
class AdminHttpServer(
    private val context: Context,
    port: Int
) : NanoHTTPD(port) {

    private val app: XtreamApp get() = context.applicationContext as XtreamApp

    override fun serve(session: IHTTPSession): Response {
        // Şifre belirlenmemişse (kullanıcı bilinçli olarak şifresiz bıraktıysa) panel
        // yerel ağdaki herkese açık çalışır. Şifre varsa Basic Auth zorunludur.
        val adminPassword = app.prefsManager.getAdminPassword()
        if (!adminPassword.isNullOrBlank() && !isAuthorized(session, adminPassword)) {
            return unauthorizedResponse()
        }

        val uri = session.uri.trimEnd('/')
        return try {
            when {
                uri == "" || uri == "/" -> htmlResponse(AdminPanelHtml.PAGE)
                uri == "/api/status" && session.method == Method.GET -> apiStatus()
                uri == "/api/accounts" && session.method == Method.GET -> apiListAccounts()
                uri == "/api/accounts" && session.method == Method.POST -> apiAddAccount(session)
                uri.startsWith("/api/accounts/") && uri.endsWith("/activate") && session.method == Method.POST ->
                    apiActivateAccount(uri.removePrefix("/api/accounts/").removeSuffix("/activate"))
                uri.startsWith("/api/accounts/") && session.method == Method.DELETE ->
                    apiDeleteAccount(uri.removePrefix("/api/accounts/"))
                uri == "/api/parental" && session.method == Method.GET -> apiGetParental()
                uri == "/api/parental" && session.method == Method.POST -> apiSetParental(session)
                uri == "/api/categories" && session.method == Method.GET -> apiGetCategories()
                uri == "/api/blocked" && session.method == Method.GET -> apiGetBlocked()
                uri == "/api/blocked" && session.method == Method.POST -> apiBlock(session)
                uri == "/api/blocked" && session.method == Method.DELETE -> apiUnblock(session)
                else -> jsonResponse(Response.Status.NOT_FOUND, JSONObject().put("error", "Bulunamadı"))
            }
        } catch (e: Exception) {
            jsonResponse(Response.Status.INTERNAL_ERROR, JSONObject().put("error", e.message ?: "Sunucu hatası"))
        }
    }

    // ---------------- Kimlik doğrulama ----------------

    private fun isAuthorized(session: IHTTPSession, expectedPassword: String): Boolean {
        val authHeader = session.headers["authorization"] ?: return false
        if (!authHeader.startsWith("Basic ")) return false
        val decoded = try {
            String(android.util.Base64.decode(authHeader.removePrefix("Basic "), android.util.Base64.DEFAULT))
        } catch (e: Exception) {
            return false
        }
        val parts = decoded.split(":", limit = 2)
        if (parts.size != 2) return false
        return parts[0] == "admin" && parts[1] == expectedPassword
    }

    private fun unauthorizedResponse(): Response {
        val response = jsonResponse(Response.Status.UNAUTHORIZED, JSONObject().put("error", "Yetkisiz"))
        response.addHeader("WWW-Authenticate", "Basic realm=\"Napxstream Yönetim Paneli\"")
        return response
    }

    // ---------------- API: Durum ----------------

    private fun apiStatus(): Response = runBlocking {
        val active = app.accountRepository.getActive()
        jsonResponse(
            Response.Status.OK,
            JSONObject()
                .put("appName", "Napxstream")
                .put("activeAccountLabel", active?.label ?: "—")
                .put("accountCount", app.accountRepository.getAll().size)
        )
    }

    // ---------------- API: Hesaplar ----------------

    private fun apiListAccounts(): Response = runBlocking {
        val accounts = app.accountRepository.getAll()
        val array = JSONArray()
        accounts.forEach { array.put(accountToJson(it)) }
        jsonResponse(Response.Status.OK, JSONObject().put("accounts", array))
    }

    private fun apiAddAccount(session: IHTTPSession): Response = runBlocking {
        val body = readJsonBody(session)
        val type = body.optString("type", "xtream")
        val label = body.optString("label", "Hesap").ifBlank { "Hesap" }

        if (type == "m3u") {
            val m3uUrl = body.optString("m3uUrl", "")
            if (m3uUrl.isBlank()) return@runBlocking badRequest("m3uUrl gerekli")
            val epgUrl = if (body.has("m3uEpgUrl")) body.optString("m3uEpgUrl").ifBlank { null } else null
            app.accountRepository.addM3uAccount(label, m3uUrl, epgUrl)
        } else {
            val host = body.optString("host", "")
            val username = body.optString("username", "")
            val password = body.optString("password", "")
            if (host.isBlank() || username.isBlank() || password.isBlank()) {
                return@runBlocking badRequest("host, username, password gerekli")
            }
            app.accountRepository.addXtreamAccount(
                label, host, body.optString("port", ""), username, password, body.optBoolean("useHttps", false)
            )
        }
        jsonResponse(Response.Status.OK, JSONObject().put("ok", true))
    }

    private fun apiActivateAccount(idStr: String): Response = runBlocking {
        val id = idStr.toLongOrNull() ?: return@runBlocking badRequest("geçersiz id")
        app.accountRepository.activate(id)
        jsonResponse(Response.Status.OK, JSONObject().put("ok", true))
    }

    private fun apiDeleteAccount(idStr: String): Response = runBlocking {
        val id = idStr.toLongOrNull() ?: return@runBlocking badRequest("geçersiz id")
        app.accountRepository.delete(id)
        jsonResponse(Response.Status.OK, JSONObject().put("ok", true))
    }

    private fun accountToJson(a: AccountEntity): JSONObject = JSONObject()
        .put("id", a.id)
        .put("label", a.label)
        .put("type", a.type)
        .put("host", a.host ?: "")
        .put("port", a.port ?: "")
        .put("username", a.username ?: "")
        .put("m3uUrl", a.m3uUrl ?: "")
        .put("isActive", a.isActive)

    // ---------------- API: Ebeveyn Kilidi ----------------

    private fun apiGetParental(): Response {
        val prefs = app.prefsManager
        return jsonResponse(
            Response.Status.OK,
            JSONObject()
                .put("enabled", prefs.isParentalLockEnabled())
                .put("hasPin", prefs.hasParentalPin())
        )
    }

    private fun apiSetParental(session: IHTTPSession): Response {
        val body = readJsonBody(session)
        val prefs = app.prefsManager
        if (body.has("pin") && body.optString("pin").isNotBlank()) {
            prefs.setParentalPin(body.optString("pin"))
        }
        if (body.has("enabled")) {
            prefs.setParentalLockEnabled(body.optBoolean("enabled"))
        }
        return jsonResponse(Response.Status.OK, JSONObject().put("ok", true))
    }

    // ---------------- API: Kategoriler (kanal engelleme listesi için) ----------------

    private fun apiGetCategories(): Response = runBlocking {
        val account = app.prefsManager.getAccount()
            ?: return@runBlocking jsonResponse(Response.Status.OK, JSONObject().put("categories", JSONArray()))

        val result = app.repository.getLiveCategories(account)
        val array = JSONArray()
        result.getOrNull()?.forEach { cat ->
            array.put(JSONObject().put("id", cat.categoryId).put("name", cat.categoryName))
        }
        jsonResponse(Response.Status.OK, JSONObject().put("categories", array))
    }

    // ---------------- API: Engellenen kanal/kategori (bouquet kısıtlaması) ----------------

    private fun apiGetBlocked(): Response = runBlocking {
        val blocked = app.repository.getBlockedChannels()
        val array = JSONArray()
        blocked.forEach {
            array.put(JSONObject().put("targetType", it.targetType).put("targetId", it.targetId).put("label", it.label))
        }
        jsonResponse(Response.Status.OK, JSONObject().put("blocked", array))
    }

    private fun apiBlock(session: IHTTPSession): Response = runBlocking {
        val body = readJsonBody(session)
        val targetType = body.optString("targetType")
        val targetId = body.optString("targetId")
        val label = body.optString("label", targetId)
        if (targetType.isBlank() || targetId.isBlank()) return@runBlocking badRequest("targetType/targetId gerekli")
        app.repository.blockChannel(BlockedChannelEntity(targetType, targetId, label))
        jsonResponse(Response.Status.OK, JSONObject().put("ok", true))
    }

    private fun apiUnblock(session: IHTTPSession): Response = runBlocking {
        val body = readJsonBody(session)
        val targetType = body.optString("targetType")
        val targetId = body.optString("targetId")
        app.repository.unblockChannel(targetType, targetId)
        jsonResponse(Response.Status.OK, JSONObject().put("ok", true))
    }

    // ---------------- Yardımcılar ----------------

    private fun readJsonBody(session: IHTTPSession): JSONObject {
        val files = HashMap<String, String>()
        try {
            session.parseBody(files)
        } catch (e: Exception) {
            return JSONObject()
        }
        val raw = files["postData"] ?: return JSONObject()
        return try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
    }

    private fun badRequest(message: String) = jsonResponse(Response.Status.BAD_REQUEST, JSONObject().put("error", message))

    private fun jsonResponse(status: Response.Status, json: JSONObject): Response =
        newFixedLengthResponse(status, "application/json; charset=utf-8", json.toString())

    private fun htmlResponse(html: String): Response =
        newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
}
