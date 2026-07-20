package com.napxstream.ui.main

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.napxstream.R
import com.napxstream.XtreamApp
import com.napxstream.ui.favorites.FavoritesFragment
import com.napxstream.ui.live.LiveFragment
import com.napxstream.ui.login.LoginActivity
import com.napxstream.ui.m3u.M3uBrowseFragment
import com.napxstream.ui.search.SearchFragment
import com.napxstream.ui.series.SeriesFragment
import com.napxstream.ui.series.SeriesMasterDetailFragment
import com.napxstream.ui.settings.SettingsFragment
import com.napxstream.ui.vod.VodFragment
import com.napxstream.ui.vod.VodMasterDetailFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as XtreamApp
        if (!app.prefsManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        if (isTvDevice()) {
            setupTvNavigation()
        } else {
            setupPhoneNavigation()
        }

        if (savedInstanceState == null) {
            showFragment(initialFragmentFromShortcut())
        }
    }

    private fun isM3u(): Boolean = (application as XtreamApp).prefsManager.isM3uSource()

    /** Uygulama kısayoluyla açıldıysa ilgili sekmeyi, aksi halde varsayılan Canlı TV'yi döndürür. */
    private fun initialFragmentFromShortcut(): Fragment {
        return when (intent?.getStringExtra(com.napxstream.util.Constants.EXTRA_SHORTCUT_TARGET)) {
            "vod" -> createVodFragment()
            "search" -> SearchFragment()
            "live" -> createLiveFragment()
            else -> createLiveFragment()
        }
    }

    /** Tablet ve geniş ekranlarda (sw600dp+) master-detail düzeni kullanılır (yalnızca Xtream'de). */
    private fun isWideScreen(): Boolean = resources.configuration.screenWidthDp >= 600

    private fun createLiveFragment(): Fragment =
        if (isM3u()) M3uBrowseFragment.newInstance(com.napxstream.util.Constants.CONTENT_TYPE_LIVE) else LiveFragment()

    private fun createVodFragment(): Fragment = when {
        isM3u() -> M3uBrowseFragment.newInstance(com.napxstream.util.Constants.CONTENT_TYPE_VOD)
        isWideScreen() -> VodMasterDetailFragment()
        else -> VodFragment()
    }

    private fun createSeriesFragment(): Fragment = when {
        isM3u() -> M3uBrowseFragment.newInstance(com.napxstream.util.Constants.CONTENT_TYPE_SERIES)
        isWideScreen() -> SeriesMasterDetailFragment()
        else -> SeriesFragment()
    }

    private fun setupPhoneNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_live -> createLiveFragment()
                R.id.nav_vod -> createVodFragment()
                R.id.nav_series -> createSeriesFragment()
                R.id.nav_favorites -> FavoritesFragment()
                R.id.nav_search -> SearchFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> createLiveFragment()
            }
            showFragment(fragment)
            true
        }
    }

    private fun setupTvNavigation() {
        findViewById<android.widget.TextView>(R.id.navLive).setOnClickListener { showFragment(createLiveFragment()) }
        findViewById<android.widget.TextView>(R.id.navVod).setOnClickListener { showFragment(createVodFragment()) }
        findViewById<android.widget.TextView>(R.id.navSeries).setOnClickListener { showFragment(createSeriesFragment()) }
        findViewById<android.widget.TextView>(R.id.navFavorites).setOnClickListener { showFragment(FavoritesFragment()) }
        findViewById<android.widget.TextView>(R.id.navSearch).setOnClickListener { showFragment(SearchFragment()) }
        findViewById<android.widget.TextView>(R.id.navSettings).setOnClickListener { showFragment(SettingsFragment()) }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
