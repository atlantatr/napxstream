package com.napxstream.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        WatchProgressEntity::class,
        TmdbCacheEntity::class,
        M3uEntryEntity::class,
        AccountEntity::class,
        BlockedChannelEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun tmdbCacheDao(): TmdbCacheDao
    abstract fun m3uEntryDao(): M3uEntryDao
    abstract fun accountDao(): AccountDao
    abstract fun blockedChannelDao(): BlockedChannelDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "napxstream.db"
                )
                    // v3 -> v4: çoklu hesap yönetimi (AccountEntity) ve kanal/kategori
                    // engelleme (BlockedChannelEntity) — yerel yönetim paneli için eklendi.
                    // Uygulama henüz yayınlanmadığı için yıkıcı migration kullanılıyor.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
