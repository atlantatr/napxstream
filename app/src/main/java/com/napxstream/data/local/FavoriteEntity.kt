package com.napxstream.data.local

import androidx.room.Entity

/** type: "live", "vod", "series" */
@Entity(tableName = "favorites", primaryKeys = ["streamId", "type"])
data class FavoriteEntity(
    val streamId: Int,
    val type: String,
    val name: String,
    val iconUrl: String?,
    val categoryId: String?,
    /** M3U kaynaklı favorilerde true; Xtream'de false. */
    val isM3u: Boolean = false,
    /** M3U canlı yayınları doğrudan bu URL ile oynatılır (Xtream'de null, URL hesap bilgisinden üretilir). */
    val streamUrl: String? = null,
    /** M3U film/dizi girişleri için orijinal entryId (detay ekranında playlist'ten yeniden bulmak için). */
    val m3uEntryId: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
