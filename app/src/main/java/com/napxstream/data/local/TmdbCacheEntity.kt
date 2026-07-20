package com.napxstream.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TMDB araması sonucu bulunan zenginleştirilmiş bilgiyi önbellekler; aynı film/dizi
 * için her seferinde yeniden arama yapmayı (ve API kotasını tüketmeyi) önler.
 * cacheKey: "movie:{normalizedTitle}:{year}" veya "tv:{normalizedTitle}"
 */
@Entity(tableName = "tmdb_cache")
data class TmdbCacheEntity(
    @PrimaryKey val cacheKey: String,
    val tmdbId: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double,
    val year: String,
    val genres: String,
    val castJoined: String, // virgülle ayrılmış oyuncu isimleri
    val trailerUrl: String?,
    /** Arama sonucu bulunamadıysa da (negatif sonuç) önbelleğe alınır ki tekrar denenmesin */
    val notFound: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)
