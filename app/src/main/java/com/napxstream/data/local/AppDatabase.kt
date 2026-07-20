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
        M3uEntryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun tmdbCacheDao(): TmdbCacheDao
    abstract fun m3uEntryDao(): M3uEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "napxstream.db"
                )
                    // v2 -> v3: TMDB önbelleği, M3U playlist önbelleği ve FavoriteEntity'ye
                    // M3U kaynaklı favoriler için isM3u/streamUrl/m3uEntryId alanları eklendi.
                    // Uygulama henüz yayınlanmadığı için yıkıcı migration kullanılıyor.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
