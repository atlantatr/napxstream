package com.napxstream.data.repository

import com.napxstream.data.api.XtreamAccount
import com.napxstream.data.api.XtreamClient
import com.napxstream.data.local.AppDatabase
import com.napxstream.data.local.FavoriteEntity
import com.napxstream.data.local.WatchProgressEntity
import com.napxstream.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Tüm Xtream Codes API çağrılarının ve yerel (Room) verinin tek giriş noktası.
 * ViewModel'lar bu sınıf üzerinden veri ister; ağ hataları burada yakalanıp
 * anlamlı Exception mesajlarına çevrilir.
 */
class XtreamRepository(private val db: AppDatabase) {

    private val api = XtreamClient.apiService

    // ---------------- Giriş ----------------
    suspend fun login(account: XtreamAccount): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(account.playerApiUrl())
            if (response.userInfo?.auth == 1 || response.userInfo?.status == "Active") {
                Result.success(response)
            } else {
                val reason = response.userInfo?.status ?: "Kimlik doğrulama başarısız"
                Result.failure(Exception("Giriş başarısız: $reason"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sunucuya bağlanılamadı: ${e.message}"))
        }
    }

    // ---------------- Canlı TV ----------------
    suspend fun getLiveCategories(account: XtreamAccount): Result<List<Category>> = safeCall {
        api.getLiveCategories(account.playerApiUrl("get_live_categories"))
    }

    suspend fun getLiveStreams(account: XtreamAccount, categoryId: String? = null): Result<List<LiveStream>> = safeCall {
        val params = if (categoryId != null) mapOf("category_id" to categoryId) else emptyMap()
        api.getLiveStreams(account.playerApiUrl("get_live_streams", params))
    }

    // ---------------- VOD ----------------
    suspend fun getVodCategories(account: XtreamAccount): Result<List<Category>> = safeCall {
        api.getVodCategories(account.playerApiUrl("get_vod_categories"))
    }

    suspend fun getVodStreams(account: XtreamAccount, categoryId: String? = null): Result<List<VodStream>> = safeCall {
        val params = if (categoryId != null) mapOf("category_id" to categoryId) else emptyMap()
        api.getVodStreams(account.playerApiUrl("get_vod_streams", params))
    }

    suspend fun getVodInfo(account: XtreamAccount, streamId: Int): Result<VodInfoResponse> = safeCall {
        api.getVodInfo(account.playerApiUrl("get_vod_info", mapOf("vod_id" to streamId.toString())))
    }

    // ---------------- Diziler ----------------
    suspend fun getSeriesCategories(account: XtreamAccount): Result<List<Category>> = safeCall {
        api.getSeriesCategories(account.playerApiUrl("get_series_categories"))
    }

    suspend fun getSeriesList(account: XtreamAccount, categoryId: String? = null): Result<List<SeriesItem>> = safeCall {
        val params = if (categoryId != null) mapOf("category_id" to categoryId) else emptyMap()
        api.getSeries(account.playerApiUrl("get_series", params))
    }

    suspend fun getSeriesInfo(account: XtreamAccount, seriesId: Int): Result<SeriesInfoResponse> = safeCall {
        api.getSeriesInfo(account.playerApiUrl("get_series_info", mapOf("series_id" to seriesId.toString())))
    }

    // ---------------- EPG ----------------
    suspend fun getShortEpg(account: XtreamAccount, streamId: Int, limit: Int = 4): Result<EpgResponse> = safeCall {
        api.getShortEpg(
            account.playerApiUrl(
                "get_short_epg",
                mapOf("stream_id" to streamId.toString(), "limit" to limit.toString())
            )
        )
    }

    /**
     * Çoklu kanal EPG grid'i için: verilen kanalların programlarını eşzamanlı (paralel) çeker.
     * Tek tek sıralı çağırmak yerine coroutine ile paralelleştirir, grid daha hızlı dolar.
     */
    suspend fun getShortEpgForChannels(
        account: XtreamAccount,
        streamIds: List<Int>,
        limit: Int = 8
    ): Map<Int, List<EpgListing>> = withContext(Dispatchers.IO) {
        streamIds.map { id ->
            async {
                val listings = getShortEpg(account, id, limit).getOrNull()?.epgListings ?: emptyList()
                id to listings
            }
        }.awaitAll().toMap()
    }

    // ---------------- Favoriler (Room) ----------------
    fun getFavorites(type: String) = db.favoriteDao().getFavoritesByType(type)

    suspend fun isFavorite(streamId: Int, type: String) = withContext(Dispatchers.IO) {
        db.favoriteDao().isFavorite(streamId, type)
    }

    suspend fun toggleFavorite(entity: FavoriteEntity, isCurrentlyFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (isCurrentlyFavorite) {
            db.favoriteDao().removeFavorite(entity.streamId, entity.type)
        } else {
            db.favoriteDao().addFavorite(entity)
        }
    }

    // ---------------- İzleme ilerlemesi ----------------
    fun getContinueWatching() = db.watchProgressDao().getContinueWatching()

    fun getContinueWatchingByType(type: String) = db.watchProgressDao().getContinueWatchingByType(type)

    suspend fun saveProgress(progress: WatchProgressEntity) = withContext(Dispatchers.IO) {
        db.watchProgressDao().saveProgress(progress)
    }

    suspend fun getProgress(contentId: String) = withContext(Dispatchers.IO) {
        db.watchProgressDao().getProgress(contentId)
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(Exception("Veri alınamadı: ${e.message}"))
        }
    }
}
