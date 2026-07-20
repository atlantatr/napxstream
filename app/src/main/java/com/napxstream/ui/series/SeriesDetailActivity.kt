package com.napxstream.ui.series

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.napxstream.R
import com.napxstream.util.Constants

/**
 * İnce bir host Activity: gerçek dizi detay mantığı SeriesDetailFragment içinde yaşar,
 * böylece aynı Fragment tablet master-detail panelinde de yeniden kullanılabilir.
 */
class SeriesDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_host)

        val seriesId = intent.getIntExtra(Constants.EXTRA_SERIES_ID, -1)
        val title = intent.getStringExtra(Constants.EXTRA_TITLE) ?: ""

        if (savedInstanceState == null) {
            val fragment = SeriesDetailFragment.newInstance(seriesId, title)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }
}
