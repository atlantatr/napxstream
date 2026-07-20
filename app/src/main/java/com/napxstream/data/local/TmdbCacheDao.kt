package com.napxstream.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TmdbCacheDao {

    @Query("SELECT * FROM tmdb_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): TmdbCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: TmdbCacheEntity)
}
