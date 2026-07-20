package com.napxstream.data.api

import com.napxstream.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Tüm istekler player_api.php üzerinden "action" parametresiyle yapılır.
 * Xtream Codes standardı: http://host:port/player_api.php?username=..&password=..&action=..
 */
interface XtreamApiService {

    // ---- Giriş / hesap doğrulama ----
    @GET
    suspend fun login(@Url fullUrl: String): LoginResponse

    // ---- Canlı TV ----
    @GET
    suspend fun getLiveCategories(@Url fullUrl: String): List<Category>

    @GET
    suspend fun getLiveStreams(@Url fullUrl: String): List<LiveStream>

    // ---- Filmler (VOD) ----
    @GET
    suspend fun getVodCategories(@Url fullUrl: String): List<Category>

    @GET
    suspend fun getVodStreams(@Url fullUrl: String): List<VodStream>

    @GET
    suspend fun getVodInfo(@Url fullUrl: String): VodInfoResponse

    // ---- Diziler ----
    @GET
    suspend fun getSeriesCategories(@Url fullUrl: String): List<Category>

    @GET
    suspend fun getSeries(@Url fullUrl: String): List<SeriesItem>

    @GET
    suspend fun getSeriesInfo(@Url fullUrl: String): SeriesInfoResponse

    // ---- EPG ----
    @GET
    suspend fun getShortEpg(@Url fullUrl: String): EpgResponse
}
