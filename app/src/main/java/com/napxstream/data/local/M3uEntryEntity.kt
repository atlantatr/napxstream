package com.napxstream.data.local

import androidx.room.Entity

/**
 * M3U kaynağından çekilip parse edilen playlist girişlerinin önbelleği. M3U'nun
 * Xtream gibi ayrı "kategori/stream" API'leri olmadığı için tüm playlist tek
 * seferde indirilip parse edilir ve burada saklanır; kullanıcı "yenile" demeden
 * her açılışta yeniden indirip parse etmeyi önler.
 */
@Entity(tableName = "m3u_entries", primaryKeys = ["playlistUrl", "entryId"])
data class M3uEntryEntity(
    val playlistUrl: String,
    /** tvg-id varsa o, yoksa stream URL'in hash'i */
    val entryId: String,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String,
    val streamUrl: String,
    /** "live" | "vod" | "series" — group-title'daki anahtar kelimelerden tahmin edilir */
    val contentType: String,
    val cachedAt: Long = System.currentTimeMillis()
)
