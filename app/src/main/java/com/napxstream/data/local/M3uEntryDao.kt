package com.napxstream.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface M3uEntryDao {

    @Query("SELECT * FROM m3u_entries WHERE playlistUrl = :playlistUrl ORDER BY groupTitle, name")
    suspend fun getAll(playlistUrl: String): List<M3uEntryEntity>

    @Query("SELECT DISTINCT groupTitle FROM m3u_entries WHERE playlistUrl = :playlistUrl ORDER BY groupTitle")
    suspend fun getGroups(playlistUrl: String): List<String>

    @Query("SELECT * FROM m3u_entries WHERE playlistUrl = :playlistUrl AND groupTitle = :group ORDER BY name")
    suspend fun getByGroup(playlistUrl: String, group: String): List<M3uEntryEntity>

    @Query("SELECT * FROM m3u_entries WHERE playlistUrl = :playlistUrl AND entryId = :entryId LIMIT 1")
    suspend fun getById(playlistUrl: String, entryId: String): M3uEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<M3uEntryEntity>)

    @Query("DELETE FROM m3u_entries WHERE playlistUrl = :playlistUrl")
    suspend fun clear(playlistUrl: String)

    @Query("SELECT COUNT(*) FROM m3u_entries WHERE playlistUrl = :playlistUrl")
    suspend fun count(playlistUrl: String): Int
}
