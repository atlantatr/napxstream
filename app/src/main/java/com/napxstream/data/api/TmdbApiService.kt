package com.napxstream.data.api

import com.napxstream.data.model.TmdbMovieDetail
import com.napxstream.data.model.TmdbMovieSearchResult
import com.napxstream.data.model.TmdbSearchResponse
import com.napxstream.data.model.TmdbTvDetail
import com.napxstream.data.model.TmdbTvSearchResult
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB (The Movie Database) v3 API. Kullanıcı kendi ücretsiz API anahtarını
 * (themoviedb.org/settings/api) Ayarlar ekranından girer; bu isteğe bağlı bir
 * zenginleştirmedir, Napxstream'in kendisi TMDB ile ilişkili/onaylı değildir.
 */
interface TmdbApiService {

    @GET("search/movie")
    suspend fun searchMovie(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("year") year: Int? = null,
        @Query("language") language: String = "tr-TR"
    ): TmdbSearchResponse<TmdbMovieSearchResult>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "tr-TR"
    ): TmdbSearchResponse<TmdbTvSearchResult>

    @GET("movie/{id}")
    suspend fun getMovieDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "credits,videos",
        @Query("language") language: String = "tr-TR"
    ): TmdbMovieDetail

    @GET("tv/{id}")
    suspend fun getTvDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") append: String = "credits,videos",
        @Query("language") language: String = "tr-TR"
    ): TmdbTvDetail

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
        const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w1280"
    }
}
