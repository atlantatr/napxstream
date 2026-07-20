package com.napxstream

import android.app.Application
import com.napxstream.data.local.AppDatabase
import com.napxstream.data.repository.M3uRepository
import com.napxstream.data.repository.TmdbRepository
import com.napxstream.data.repository.XtreamRepository
import com.napxstream.util.PrefsManager

class XtreamApp : Application() {

    lateinit var repository: XtreamRepository
        private set

    lateinit var tmdbRepository: TmdbRepository
        private set

    lateinit var m3uRepository: M3uRepository
        private set

    lateinit var prefsManager: PrefsManager
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = XtreamRepository(db)
        tmdbRepository = TmdbRepository(db)
        m3uRepository = M3uRepository(db)
        prefsManager = PrefsManager(this)
    }
}
