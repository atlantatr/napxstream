package com.napxstream.data.repository

import com.napxstream.data.api.TmdbApiService
import com.napxstream.data.api.TmdbClient
import com.napxstream.data.local.AppDatabase
import com.napxstream.data.local.TmdbCacheEntity
import com.napxstream.data.model.TmdbEnrichedInfo
import com.napxstream.data.model.TmdbVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * TMDB ile film/dizi zenginleştirme. Kullanıcı Ayarlar'dan kendi ücretsiz API
 * anahtarını girmediği sürece hiçbir çağrı yapılmaz (isteğe bağlı özellik).
 * Sonuçlar Room'da önbelleklenir; "bulunamadı" sonucu da önbelleğe alınır ki
 * her ekran açılışında aynı başarısız arama tekrarlanmasın.
 */
class TmdbRepository(private val db: AppDatabase) {

    private val api: TmdbApiService = TmdbClient.apiService
    private val cacheDao = db.tmdbCacheDao()

    /** Film için TMDB zenginleştirmesi. apiKey boşsa null döner (sessizce atlanır). */
    suspend fun enrichMovie(apiKey: String?, title: String, year: String?): TmdbEnrichedInfo? {
        if (apiKey.isNullOrBlank() || title.isBlank()) return null
        val cacheKey = "movie:${normalize(title)}:${year ?: ""}"
        return withContext(Dispatchers.IO) {
            val cachedResult = cached(cacheKey)
            if (cachedResult != null || wasCachedAsNotFound(cacheKey)) return@withContext cachedResult

            try {
                val searchResult = api.searchMovie(apiKey, title, year?.toIntOrNull())
                    .results?.firstOrNull()
                if (searchResult == null) {
                    cacheNotFound(cacheKey)
                    return@withContext null
                }
                val detail = api.getMovieDetail(searchResult.id, apiKey)
                val info = TmdbEnrichedInfo(
                    tmdbId = detail.id,
                    title = detail.title ?: title,
                    overview = detail.overview ?: "",
                    posterUrl = detail.posterPath?.let { TmdbApiService.IMAGE_BASE_URL + it },
                    backdropUrl = detail.backdropPath?.let { TmdbApiService.BACKDROP_BASE_URL + it },
                    rating = detail.voteAverage ?: 0.0,
                    year = detail.releaseDate?.take(4) ?: "",
                    genres = detail.genres?.mapNotNull { it.name }?.joinToString(", ") ?: "",
                    cast = detail.credits?.cast?.take(6)?.mapNotNull { it.name } ?: emptyList(),
                    trailerUrl = findTrailerUrl(detail.videos?.results)
                )
                cachePut(cacheKey, info)
                info
            } catch (e: Exception) {
                null // Ağ/API hatasında sessizce vazgeç; ekran Xtream verisiyle çalışmaya devam eder
            }
        }
    }

    /** Dizi için TMDB zenginleştirmesi. */
    suspend fun enrichSeries(apiKey: String?, title: String): TmdbEnrichedInfo? {
        if (apiKey.isNullOrBlank() || title.isBlank()) return null
        val cacheKey = "tv:${normalize(title)}"
        return withContext(Dispatchers.IO) {
            val cachedResult = cached(cacheKey)
            if (cachedResult != null || wasCachedAsNotFound(cacheKey)) return@withContext cachedResult

            try {
                val searchResult = api.searchTv(apiKey, title).results?.firstOrNull()
                if (searchResult == null) {
                    cacheNotFound(cacheKey)
                    return@withContext null
                }
                val detail = api.getTvDetail(searchResult.id, apiKey)
                val info = TmdbEnrichedInfo(
                    tmdbId = detail.id,
                    title = detail.name ?: title,
                    overview = detail.overview ?: "",
                    posterUrl = detail.posterPath?.let { TmdbApiService.IMAGE_BASE_URL + it },
                    backdropUrl = detail.backdropPath?.let { TmdbApiService.BACKDROP_BASE_URL + it },
                    rating = detail.voteAverage ?: 0.0,
                    year = detail.firstAirDate?.take(4) ?: "",
                    genres = detail.genres?.mapNotNull { it.name }?.joinToString(", ") ?: "",
                    cast = detail.credits?.cast?.take(6)?.mapNotNull { it.name } ?: emptyList(),
                    trailerUrl = findTrailerUrl(detail.videos?.results)
                )
                cachePut(cacheKey, info)
                info
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun cached(key: String): TmdbEnrichedInfo? {
        val entity = cacheDao.get(key) ?: return null
        if (entity.notFound) return null
        return TmdbEnrichedInfo(
            tmdbId = entity.tmdbId,
            title = entity.title,
            overview = entity.overview,
            posterUrl = entity.posterUrl,
            backdropUrl = entity.backdropUrl,
            rating = entity.rating,
            year = entity.year,
            genres = entity.genres,
            cast = entity.castJoined.split(",").filter { it.isNotBlank() },
            trailerUrl = entity.trailerUrl
        )
    }

    private suspend fun wasCachedAsNotFound(key: String): Boolean = cacheDao.get(key)?.notFound == true

    private suspend fun cachePut(key: String, info: TmdbEnrichedInfo) {
        cacheDao.put(
            TmdbCacheEntity(
                cacheKey = key,
                tmdbId = info.tmdbId,
                title = info.title,
                overview = info.overview,
                posterUrl = info.posterUrl,
                backdropUrl = info.backdropUrl,
                rating = info.rating,
                year = info.year,
                genres = info.genres,
                castJoined = info.cast.joinToString(","),
                trailerUrl = info.trailerUrl
            )
        )
    }

    private suspend fun cacheNotFound(key: String) {
        cacheDao.put(
            TmdbCacheEntity(
                cacheKey = key, tmdbId = -1, title = "", overview = "",
                posterUrl = null, backdropUrl = null, rating = 0.0, year = "",
                genres = "", castJoined = "", trailerUrl = null, notFound = true
            )
        )
    }

    private fun findTrailerUrl(videos: List<TmdbVideo>?): String? {
        val trailer = videos?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }
            ?: videos?.firstOrNull { it.site == "YouTube" }
        return trailer?.key?.let { "https://www.youtube.com/watch?v=$it" }
    }

    private fun normalize(title: String): String =
        title.lowercase(Locale.getDefault()).trim().replace(Regex("[^a-z0-9ığüşöç ]"), "")
}
