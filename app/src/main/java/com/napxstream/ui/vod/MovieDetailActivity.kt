package com.napxstream.ui.vod

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.napxstream.R
import com.napxstream.util.Constants

/**
 * İnce bir host Activity: gerçek film detay mantığı MovieDetailFragment içinde yaşar,
 * böylece aynı Fragment tablet master-detail panelinde de yeniden kullanılabilir.
 */
class MovieDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_host)

        val streamId = intent.getIntExtra(Constants.EXTRA_STREAM_ID, -1)
        val title = intent.getStringExtra(Constants.EXTRA_TITLE) ?: ""

        if (savedInstanceState == null) {
            val fragment = MovieDetailFragment.newInstance(streamId, title)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }
}
