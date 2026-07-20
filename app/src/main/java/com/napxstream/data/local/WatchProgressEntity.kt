package com.napxstream.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** contentId: VOD için streamId, dizi için episodeId (String) */
@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val contentId: String,
    val type: String, // "vod" | "series"
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    /** Dizi bölümlerinde üst diziyi (seriesId) tutar; "İzlemeye Devam Et" tıklanınca dizi
     * detayına dönebilmek için gerekli. VOD içerikler için kullanılmaz (null). */
    val parentId: String? = null,
    /** Devam Et şeridinde afiş göstermek için; her zaman mevcut olmayabilir. */
    val posterUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
