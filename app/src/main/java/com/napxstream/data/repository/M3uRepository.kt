package com.napxstream.data.repository

import com.napxstream.data.local.AppDatabase
import com.napxstream.data.local.M3uEntryEntity
import com.napxstream.data.model.M3uEntry
import com.napxstream.util.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * M3U playlist kaynağı için Xtream'e alternatif repository. Xtream'in aksine tek
 * bir düz metin dosyası indirilip tamamı bir kerede parse edilir; sonuç Room'da
 * playlist URL'ine göre önbelleklenir (bkz. M3uEntryEntity) ki her ekran açılışında
 * -bazen birkaç MB olabilen- playlist yeniden indirilmesin.
 */
class M3uRepository(private val db: AppDatabase) {

    private val dao = db.m3uEntryDao()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Playlist'i (gerekirse) indirir ve önbelleğe alır.
     * @param forceRefresh true ise önbellek doluyken bile yeniden indirir (Ayarlar > Yenile).
     */
    suspend fun ensureLoaded(playlistUrl: String, forceRefresh: Boolean = false): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                if (!forceRefresh) {
                    val existing = dao.count(playlistUrl)
                    if (existing > 0) return@withContext Result.success(existing)
                }

                val request = Request.Builder().url(playlistUrl).build()
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Playlist indirilemedi: HTTP ${response.code}"))
                }
                val text = response.body?.string() ?: return@withContext Result.failure(Exception("Playlist boş"))

                val parseResult = M3uParser.parse(text)
                if (parseResult.entries.isEmpty()) {
                    return@withContext Result.failure(Exception("Playlist'te geçerli kanal bulunamadı. Format #EXTM3U olmalı."))
                }

                val entities = parseResult.entries.map {
                    M3uEntryEntity(
                        playlistUrl = playlistUrl,
                        entryId = it.entryId,
                        name = it.name,
                        logoUrl = it.logoUrl,
                        groupTitle = it.groupTitle,
                        streamUrl = it.streamUrl,
                        contentType = it.contentType
                    )
                }

                dao.clear(playlistUrl)
                dao.insertAll(entities)
                Result.success(entities.size)
            } catch (e: Exception) {
                Result.failure(Exception("Playlist alınamadı: ${e.message}"))
            }
        }

    suspend fun getGroups(playlistUrl: String, contentType: String): List<String> = withContext(Dispatchers.IO) {
        dao.getAll(playlistUrl)
            .filter { it.contentType == contentType }
            .map { it.groupTitle }
            .distinct()
            .sorted()
    }

    suspend fun getEntries(playlistUrl: String, contentType: String, group: String?): List<M3uEntry> =
        withContext(Dispatchers.IO) {
            val all = if (group != null) dao.getByGroup(playlistUrl, group) else dao.getAll(playlistUrl)
            all.filter { it.contentType == contentType }.map { it.toModel() }
        }

    suspend fun searchEntries(playlistUrl: String, query: String): List<M3uEntry> = withContext(Dispatchers.IO) {
        val q = query.lowercase().trim()
        if (q.isBlank()) return@withContext emptyList()
        dao.getAll(playlistUrl).filter { it.name.lowercase().contains(q) }.map { it.toModel() }
    }

    suspend fun getEntry(playlistUrl: String, entryId: String): M3uEntry? = withContext(Dispatchers.IO) {
        dao.getById(playlistUrl, entryId)?.toModel()
    }

    private fun M3uEntryEntity.toModel() = M3uEntry(
        entryId = entryId,
        name = name,
        logoUrl = logoUrl,
        groupTitle = groupTitle,
        streamUrl = streamUrl,
        contentType = contentType
    )
}
