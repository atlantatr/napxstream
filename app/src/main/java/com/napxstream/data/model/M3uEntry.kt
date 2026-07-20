package com.napxstream.data.model

/** M3U playlist'ten parse edilmiş tek bir kanal/film/dizi bölümü girişi */
data class M3uEntry(
    val entryId: String,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String,
    val streamUrl: String,
    val contentType: String // Constants.CONTENT_TYPE_LIVE | _VOD | _SERIES
)
