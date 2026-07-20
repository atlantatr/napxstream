package com.napxstream.ui.m3u

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.napxstream.R
import com.napxstream.XtreamApp
import com.napxstream.data.model.TmdbEnrichedInfo
import com.napxstream.databinding.ActivityMovieDetailBinding
import com.napxstream.ui.player.PlayerActivity
import com.napxstream.util.Constants
import kotlinx.coroutines.launch

/**
 * M3U kaynağındaki film/dizi girişleri için detay ekranı. M3U'nun kendi meta
 * verisi yoktur (sadece ad+URL), bu yüzden bu ekran tamamen TMDB zenginleştirmesine
 * dayanır — TMDB anahtarı tanımlı değilse yalnızca isim + oynat butonu gösterilir.
 * Diziler için de bölüm yapısı yoktur; TMDB anahtarıyla bulunursa genel bilgi
 * gösterilir, oynatma her zaman playlist'teki tek girişi doğrudan başlatır.
 */
class M3uDetailFragment : Fragment() {

    private var _binding: ActivityMovieDetailBinding? = null
    private val binding get() = _binding!!

    private var entryId: String = ""
    private var m3uContentType: String = Constants.CONTENT_TYPE_VOD

    var onCloseRequested: (() -> Unit)? = null
        get() = field ?: (activity as? M3uDetailActivity)?.let { { it.finish() } }

    companion object {
        fun newInstance(entryId: String, title: String, contentType: String): M3uDetailFragment {
            return M3uDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(Constants.EXTRA_M3U_ENTRY_ID, entryId)
                    putString(Constants.EXTRA_TITLE, title)
                    putString(Constants.EXTRA_M3U_CONTENT_TYPE, contentType)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMovieDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        entryId = arguments?.getString(Constants.EXTRA_M3U_ENTRY_ID) ?: ""
        val title = arguments?.getString(Constants.EXTRA_TITLE) ?: ""
        m3uContentType = arguments?.getString(Constants.EXTRA_M3U_CONTENT_TYPE) ?: Constants.CONTENT_TYPE_VOD
        binding.titleText.text = title
        binding.playButton.text = getString(R.string.play_from_start)

        if (onCloseRequested != null) {
            binding.backButton.setOnClickListener { onCloseRequested?.invoke() }
        } else {
            binding.backButton.visibility = View.GONE
        }

        val app = requireActivity().application as XtreamApp
        val playlistUrl = app.prefsManager.getM3uUrl() ?: return

        binding.playButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val entry = app.m3uRepository.getEntry(playlistUrl, entryId) ?: return@launch
                startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(Constants.EXTRA_STREAM_URL, entry.streamUrl)
                    putExtra(Constants.EXTRA_TITLE, entry.name)
                    putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_LIVE) // M3U'da tekil oynatım
                })
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            binding.loadingProgress.visibility = View.VISIBLE

            val tmdbKey = app.prefsManager.getTmdbApiKey()
            val enriched = if (tmdbKey != null) {
                if (m3uContentType == Constants.CONTENT_TYPE_SERIES) {
                    app.tmdbRepository.enrichSeries(tmdbKey, title)
                } else {
                    app.tmdbRepository.enrichMovie(tmdbKey, title, null)
                }
            } else null

            binding.loadingProgress.visibility = View.GONE

            if (enriched != null) {
                applyTmdbEnrichment(enriched)
            } else {
                binding.plotText.text = if (tmdbKey == null) {
                    getString(R.string.tmdb_hint)
                } else {
                    "TMDB'de eşleşme bulunamadı. İçerik yine de oynatılabilir."
                }
            }
        }
    }

    private fun applyTmdbEnrichment(info: TmdbEnrichedInfo) {
        val imageUrl = info.backdropUrl ?: info.posterUrl
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(this).load(imageUrl).into(binding.backdropImage)
        }
        binding.metaText.text = listOfNotNull(
            info.year.ifBlank { null },
            info.genres.ifBlank { null },
            if (info.rating > 0) "${"%.1f".format(info.rating)} ⭐" else null
        ).joinToString(" · ")
        binding.plotText.text = info.overview

        if (info.cast.isNotEmpty()) {
            binding.castLabel.visibility = View.VISIBLE
            binding.castText.visibility = View.VISIBLE
            binding.castText.text = info.cast.joinToString(", ")
        }
        if (!info.trailerUrl.isNullOrBlank()) {
            binding.trailerButton.visibility = View.VISIBLE
            binding.trailerButton.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.trailerUrl)))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
