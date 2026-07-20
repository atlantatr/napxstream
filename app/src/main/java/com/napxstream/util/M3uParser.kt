package com.napxstream.util

import com.napxstream.data.model.M3uEntry

/**
 * Standart IPTV M3U/M3U8 playlist formatını parse eder:
 *
 * #EXTM3U url-tvg="http://.../epg.xml"
 * #EXTINF:-1 tvg-id="ch1" tvg-logo="http://logo.png" group-title="Spor",Kanal Adı
 * http://sunucu/stream/kanal.m3u8
 *
 * Xtream Codes'un aksine M3U'da ayrı canlı/film/dizi API'leri yoktur; içerik tipi
 * group-title içindeki anahtar kelimelerden tahmin edilir (kesin değildir, sağlayıcıdan
 * sağlayıcıya değişebilir).
 */
object M3uParser {

    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
    private val seriesKeywords = listOf("dizi", "series", "show", "tv show", "episode", "bölüm")
    private val vodKeywords = listOf("vod", "film", "movie", "sinema", "cinema")

    data class ParseResult(val entries: List<M3uEntry>, val epgUrl: String?)

    fun parse(playlistText: String): ParseResult {
        val lines = playlistText.lines()
        val entries = mutableListOf<M3uEntry>()
        var epgUrl: String? = null

        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null
        var pendingTvgId: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTM3U")) {
                epgUrl = attrRegex.findAll(line)
                    .firstOrNull { it.groupValues[1].equals("url-tvg", true) || it.groupValues[1].equals("x-tvg-url", true) }
                    ?.groupValues?.get(2)
                continue
            }

            if (line.startsWith("#EXTINF")) {
                val attrs = attrRegex.findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                pendingTvgId = attrs["tvg-id"]?.takeIf { it.isNotBlank() }
                pendingLogo = attrs["tvg-logo"]
                pendingGroup = attrs["group-title"]?.takeIf { it.isNotBlank() } ?: "Diğer"
                // Görünen ad, son virgülden sonraki kısımdır
                pendingName = line.substringAfterLast(",").trim().ifBlank { "Adsız" }
                continue
            }

            // Yorum/etiket olmayan, boş olmayan bir satır: stream URL'i
            if (!line.startsWith("#")) {
                val name = pendingName ?: continue // #EXTINF olmadan URL varsa atla (bozuk giriş)
                val group = pendingGroup ?: "Diğer"
                val contentType = guessContentType(group, name)
                val entryId = pendingTvgId ?: line.hashCode().toString()

                entries.add(
                    M3uEntry(
                        entryId = entryId,
                        name = name,
                        logoUrl = pendingLogo,
                        groupTitle = group,
                        streamUrl = line,
                        contentType = contentType
                    )
                )

                pendingName = null; pendingLogo = null; pendingGroup = null; pendingTvgId = null
            }
        }

        return ParseResult(entries, epgUrl)
    }

    private fun guessContentType(group: String, name: String): String {
        val g = group.lowercase()
        val n = name.lowercase()
        return when {
            seriesKeywords.any { g.contains(it) } || Regex("""s\d{1,2}\s?e\d{1,3}""").containsMatchIn(n) ->
                Constants.CONTENT_TYPE_SERIES
            vodKeywords.any { g.contains(it) } -> Constants.CONTENT_TYPE_VOD
            else -> Constants.CONTENT_TYPE_LIVE
        }
    }
}
