package com.napxstream.data.api

/**
 * Kullanıcının girdiği sunucu bilgileri (host bazen "http://domain:port" formatında,
 * bazen sadece domain olarak gelir; normalizeHost bunu tolere eder).
 */
data class XtreamAccount(
    val host: String,
    val port: String = "",
    val username: String,
    val password: String,
    val useHttps: Boolean = false
) {
    /** http(s)://host:port şeklinde temel adres */
    val baseUrl: String
        get() {
            var h = host.trim()
            h = h.removeSuffix("/")
            // Kullanıcı zaten "http://" veya "https://" yazmışsa koru
            val hasScheme = h.startsWith("http://") || h.startsWith("https://")
            val scheme = if (useHttps) "https" else "http"
            val hostOnly = if (hasScheme) h.substringAfter("://") else h
            val hostWithoutPort = hostOnly.substringBefore(":")
            val portToUse = port.ifBlank { hostOnly.substringAfter(":", "") }
            return if (portToUse.isNotBlank()) {
                "$scheme://$hostWithoutPort:$portToUse"
            } else {
                "$scheme://$hostWithoutPort"
            }
        }

    fun playerApiUrl(action: String? = null, extraParams: Map<String, String> = emptyMap()): String {
        val sb = StringBuilder("$baseUrl/player_api.php?username=$username&password=$password")
        if (action != null) sb.append("&action=$action")
        extraParams.forEach { (k, v) -> sb.append("&$k=$v") }
        return sb.toString()
    }

    /** Canlı yayın stream URL'i (m3u8/ts) */
    fun liveStreamUrl(streamId: Int, extension: String = "m3u8"): String =
        "$baseUrl/live/$username/$password/$streamId.$extension"

    /** VOD (film) stream URL'i */
    fun vodStreamUrl(streamId: Int, extension: String): String =
        "$baseUrl/movie/$username/$password/$streamId.$extension"

    /** Dizi bölümü stream URL'i */
    fun seriesStreamUrl(episodeId: String, extension: String): String =
        "$baseUrl/series/$username/$password/$episodeId.$extension"

    /**
     * Arşiv/catch-up (geçmiş yayın) URL'i. Xtream Codes standart formatı:
     * /timeshift/USER/PASS/DURATION_DAKİKA/YYYY-MM-DD:HH-MM/STREAM_ID.m3u8
     * Yalnızca tv_archive=1 olan kanallarda çalışır; süre dakika cinsindendir.
     */
    fun timeshiftUrl(streamId: Int, startTimeMs: Long, durationMinutes: Int): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", java.util.Locale.US)
        val startFormatted = sdf.format(java.util.Date(startTimeMs))
        return "$baseUrl/timeshift/$username/$password/$durationMinutes/$startFormatted/$streamId.m3u8"
    }

    /** XMLTV tam EPG adresi (opsiyonel, tüm kanalların programı) */
    fun xmltvUrl(): String = "$baseUrl/xmltv.php?username=$username&password=$password"
}
