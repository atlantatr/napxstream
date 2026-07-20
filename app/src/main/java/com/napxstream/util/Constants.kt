package com.napxstream.util

import android.util.Base64
import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val CONTENT_TYPE_LIVE = "live"
    const val CONTENT_TYPE_VOD = "vod"
    const val CONTENT_TYPE_SERIES = "series"
    const val CONTENT_TYPE_ARCHIVE = "archive"

    const val EXTRA_STREAM_URL = "extra_stream_url"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_CONTENT_ID = "extra_content_id"
    const val EXTRA_CONTENT_TYPE = "extra_content_type"
    const val EXTRA_CATEGORY_ID = "extra_category_id"
    const val EXTRA_STREAM_ID = "extra_stream_id"
    const val EXTRA_SERIES_ID = "extra_series_id"
    const val EXTRA_CHANNEL_IDS = "extra_channel_ids"
    const val EXTRA_CHANNEL_NAMES = "extra_channel_names"
    const val EXTRA_CHANNEL_ARCHIVE_FLAGS = "extra_channel_archive_flags"
    const val EXTRA_FOCUS_CHANNEL_ID = "extra_focus_channel_id"
    const val EXTRA_PARENT_ID = "extra_parent_id"
    const val EXTRA_POSTER_URL = "extra_poster_url"
    const val EXTRA_IS_ARCHIVE = "extra_is_archive"
    const val EXTRA_ARCHIVE_START = "extra_archive_start"
    const val EXTRA_ARCHIVE_DURATION_MIN = "extra_archive_duration_min"
    const val EXTRA_NEXT_TITLE = "extra_next_title"
    const val EXTRA_NEXT_STREAM_URL = "extra_next_stream_url"
    const val EXTRA_NEXT_CONTENT_ID = "extra_next_content_id"
    const val EXTRA_SHORTCUT_TARGET = "extra_shortcut_target"
    const val EXTRA_M3U_ENTRY_ID = "extra_m3u_entry_id"
    const val EXTRA_M3U_CONTENT_TYPE = "extra_m3u_content_type"

    /** Xtream get_short_epg alanları base64 kodludur */
    fun decodeEpgText(encoded: String?): String {
        if (encoded.isNullOrBlank()) return ""
        return try {
            String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            encoded
        }
    }

    /** EPG unix timestamp -> "HH:mm" */
    fun formatEpgTime(timestamp: String?): String {
        val ts = timestamp?.toLongOrNull() ?: return "--:--"
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(ts * 1000))
    }
}
