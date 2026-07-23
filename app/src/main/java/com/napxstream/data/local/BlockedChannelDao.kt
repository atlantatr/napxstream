package com.napxstream.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedChannelDao {

    @Query("SELECT * FROM blocked_channels")
    fun getAllLive(): LiveData<List<BlockedChannelEntity>>

    @Query("SELECT * FROM blocked_channels")
    suspend fun getAll(): List<BlockedChannelEntity>

    @Query("SELECT targetId FROM blocked_channels WHERE targetType = 'category'")
    suspend fun getBlockedCategoryIds(): List<String>

    @Query("SELECT targetId FROM blocked_channels WHERE targetType = 'channel'")
    suspend fun getBlockedChannelIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(entity: BlockedChannelEntity)

    @Query("DELETE FROM blocked_channels WHERE targetType = :targetType AND targetId = :targetId")
    suspend fun unblock(targetType: String, targetId: String)
}
