package com.napxstream.ui.m3u

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.napxstream.R
import com.napxstream.util.Constants

class M3uDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_host)

        val entryId = intent.getStringExtra(Constants.EXTRA_M3U_ENTRY_ID) ?: ""
        val title = intent.getStringExtra(Constants.EXTRA_TITLE) ?: ""
        val contentType = intent.getStringExtra(Constants.EXTRA_M3U_CONTENT_TYPE) ?: Constants.CONTENT_TYPE_VOD

        if (savedInstanceState == null) {
            val fragment = M3uDetailFragment.newInstance(entryId, title, contentType)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }
}
