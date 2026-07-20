package com.napxstream.data.model

import com.google.gson.annotations.SerializedName

data class TmdbSearchResponse<T>(
    @SerializedName("results") val results: List<T>?
)

data class TmdbMovieSearchResult(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("poster_path") val posterPath: String?
)

data class TmdbTvSearchResult(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("poster_path") val posterPath: String?
)

data class TmdbMovieDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
    @SerializedName("credits") val credits: TmdbCredits?,
    @SerializedName("videos") val videos: TmdbVideoResponse?
)

data class TmdbTvDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
    @SerializedName("credits") val credits: TmdbCredits?,
    @SerializedName("videos") val videos: TmdbVideoResponse?
)

data class TmdbGenre(@SerializedName("name") val name: String?)

data class TmdbCredits(@SerializedName("cast") val cast: List<TmdbCastMember>?)

data class TmdbCastMember(
    @SerializedName("name") val name: String?,
    @SerializedName("character") val character: String?,
    @SerializedName("profile_path") val profilePath: String?
)

data class TmdbVideoResponse(@SerializedName("results") val results: List<TmdbVideo>?)

data class TmdbVideo(
    @SerializedName("key") val key: String?,
    @SerializedName("site") val site: String?,
    @SerializedName("type") val type: String?
)

/** Xtream verisiyle TMDB'den gelen zenginleştirmenin birleştirildiği, ekranların kullandığı ortak model. */
data class TmdbEnrichedInfo(
    val tmdbId: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double,
    val year: String,
    val genres: String,
    val cast: List<String>,
    val trailerUrl: String?
)
