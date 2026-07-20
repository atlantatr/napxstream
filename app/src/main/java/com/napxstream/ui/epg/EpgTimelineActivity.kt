package com.napxstream.ui.epg

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.napxstream.R
import com.napxstream.XtreamApp
import com.napxstream.databinding.ActivityEpgTimelineBinding
import com.napxstream.ui.player.PlayerActivity
import com.napxstream.util.Constants
import com.napxstream.util.Resource
import com.napxstream.util.ViewModelFactory
import java.util.Calendar

class EpgTimelineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEpgTimelineBinding
    private lateinit var viewModel: EpgTimelineViewModel
    private var focusChannelIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEpgTimelineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        val channelIds = intent.getIntArrayExtra(Constants.EXTRA_CHANNEL_IDS)?.toList() ?: emptyList()
        val channelNames = intent.getStringArrayExtra(Constants.EXTRA_CHANNEL_NAMES)?.toList() ?: emptyList()
        val archiveFlags = intent.getBooleanArrayExtra(Constants.EXTRA_CHANNEL_ARCHIVE_FLAGS)?.toList() ?: emptyList()
        val focusChannelId = intent.getIntExtra(Constants.EXTRA_FOCUS_CHANNEL_ID, -1)
        focusChannelIndex = channelIds.indexOf(focusChannelId).coerceAtLeast(0)

        if (channelIds.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = getString(R.string.empty_list)
            return
        }

        val app = application as XtreamApp
        val account = app.prefsManager.getAccount() ?: run { finish(); return }

        viewModel = ViewModelProvider(this, ViewModelFactory(app.repository) { EpgTimelineViewModel(it) })[EpgTimelineViewModel::class.java]

        binding.epgGridView.setOnProgramClickListener { row, program ->
            handleProgramClick(account, row, program)
        }

        binding.nowButton.setOnClickListener { binding.epgGridView.scrollToNow(focusChannelIndex) }

        viewModel.rows.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingProgress.visibility = View.VISIBLE
                    binding.emptyText.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.loadingProgress.visibility = View.GONE
                    if (resource.data.isEmpty()) {
                        binding.emptyText.visibility = View.VISIBLE
                    } else {
                        val (windowStart, windowEnd) = computeWindow()
                        binding.epgGridView.setData(resource.data, windowStart, windowEnd)
                        binding.epgGridView.post { binding.epgGridView.scrollToNow(focusChannelIndex) }
                    }
                }
                is Resource.Error -> {
                    binding.loadingProgress.visibility = View.GONE
                    binding.emptyText.visibility = View.VISIBLE
                    binding.emptyText.text = resource.message
                }
            }
        }

        viewModel.load(account, channelIds, channelNames, archiveFlags)
    }

    /**
     * Programa dokunulduğunda: hâlâ yayında olan programsa canlı yayını başlatır;
     * geçmişte kalmış bir programsa ve kanal tv_archive destekliyorsa arşivden
     * izleme seçeneği sunar; desteklenmiyorsa sadece bilgi gösterir.
     */
    private fun handleProgramClick(
        account: com.napxstream.data.api.XtreamAccount,
        row: EpgChannelRow,
        program: com.napxstream.data.model.EpgListing
    ) {
        val title = Constants.decodeEpgText(program.title)
        val startMs = (program.startTimestamp?.toLongOrNull() ?: 0L) * 1000
        val stopMs = (program.stopTimestamp?.toLongOrNull() ?: 0L) * 1000
        val nowMs = System.currentTimeMillis()
        val timeRange = getString(
            R.string.program_time_range,
            Constants.formatEpgTime(program.startTimestamp),
            Constants.formatEpgTime(program.stopTimestamp)
        )

        when {
            nowMs in startMs until stopMs -> {
                // Şu an yayında: canlı akışı doğrudan başlat
                startActivity(Intent(this, PlayerActivity::class.java).apply {
                    putExtra(Constants.EXTRA_STREAM_URL, account.liveStreamUrl(row.streamId))
                    putExtra(Constants.EXTRA_TITLE, "${row.name} — $title")
                    putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_LIVE)
                    putExtra(Constants.EXTRA_STREAM_ID, row.streamId)
                })
            }
            stopMs < nowMs && row.tvArchive -> {
                // Geçmiş program + arşiv destekli kanal: arşivden izleme sun
                AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage("${row.name} · $timeRange\n\nBu programı arşivden izlemek ister misiniz?")
                    .setPositiveButton(R.string.watch_from_archive) { _, _ ->
                        val durationMin = ((stopMs - startMs) / 60000L).toInt().coerceAtLeast(1)
                        val url = account.timeshiftUrl(row.streamId, startMs, durationMin)
                        startActivity(Intent(this, PlayerActivity::class.java).apply {
                            putExtra(Constants.EXTRA_STREAM_URL, url)
                            putExtra(Constants.EXTRA_TITLE, "${row.name} — $title")
                            putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_ARCHIVE)
                            putExtra(Constants.EXTRA_STREAM_ID, row.streamId)
                        })
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            else -> {
                Toast.makeText(this, "${row.name}: $title ($timeRange)", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Grid penceresi: şimdiden 1 saat öncesinden 5 saat sonrasına kadar */
    private fun computeWindow(): Pair<Long, Long> {
        val now = Calendar.getInstance()
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        val start = now.timeInMillis - 60 * 60 * 1000
        val end = now.timeInMillis + 5 * 60 * 60 * 1000
        return start to end
    }
}
