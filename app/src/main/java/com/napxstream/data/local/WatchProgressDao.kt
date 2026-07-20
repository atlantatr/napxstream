package com.napxstream.data.local

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WatchProgressDao {

    @Query("SELECT * FROM watch_progress WHERE contentId = :contentId LIMIT 1")
    suspend fun getProgress(contentId: String): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress ORDER BY updatedAt DESC LIMIT 20")
    fun getContinueWatching(): LiveData<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE type = :type ORDER BY updatedAt DESC LIMIT 20")
    fun getContinueWatchingByType(type: String): LiveData<List<WatchProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE contentId = :contentId")
    suspend fun clearProgress(contentId: String)
}
