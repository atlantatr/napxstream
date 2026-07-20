package com.napxstream.ui.vod

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.napxstream.R
import com.napxstream.XtreamApp
import com.napxstream.data.model.VodInfoResponse
import com.napxstream.databinding.ActivityMovieDetailBinding
import com.napxstream.ui.player.PlayerActivity
import com.napxstream.util.Constants
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

/**
 * MovieDetailActivity ile aynı iş mantığını taşır; hem tam ekran Activity içinde
 * (telefon) hem de tablet master-detail panelinde gömülü olarak kullanılabilmesi
 * için Fragment olarak çıkarılmıştır. TMDB API anahtarı Ayarlar'da tanımlıysa,
 * Xtream'in kendi (bazen zayıf) meta verisi TMDB'den gelen afiş/puan/oyuncu/fragman
 * ile zenginleştirilir.
 */
class MovieDetailFragment : Fragment() {

    private var _binding: ActivityMovieDetailBinding? = null
    private val binding get() = _binding!!

    private var streamId: Int = -1
    private var containerExtension: String = "mp4"
    private var posterUrl: String? = null
    private var trailerUrl: String? = null

    /** Gömülü (tablet) modda geri butonuna basıldığında çağrılır; null ise buton gizlenir. */
    var onCloseRequested: (() -> Unit)? = null
        get() = field ?: (activity as? MovieDetailActivity)?.let { { it.finish() } }

    companion object {
        fun newInstance(streamId: Int, title: String): MovieDetailFragment {
            return MovieDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.EXTRA_STREAM_ID, streamId)
                    putString(Constants.EXTRA_TITLE, title)
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

        streamId = arguments?.getInt(Constants.EXTRA_STREAM_ID) ?: -1
        val title = arguments?.getString(Constants.EXTRA_TITLE) ?: ""
        binding.titleText.text = title

        if (onCloseRequested != null) {
            binding.backButton.setOnClickListener { onCloseRequested?.invoke() }
        } else {
            binding.backButton.visibility = View.GONE
        }

        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            binding.loadingProgress.visibility = View.VISIBLE
            val result = app.repository.getVodInfo(account, streamId)
            binding.loadingProgress.visibility = View.GONE

            var releaseYear: String? = null

            result.onSuccess { response: VodInfoResponse ->
                containerExtension = response.movieData?.containerExtension ?: "mp4"
                val info = response.info
                posterUrl = info?.movieImage
                releaseYear = info?.releaseDate?.take(4)
                Glide.with(this@MovieDetailFragment)
                    .load(info?.movieImage)
                    .into(binding.backdropImage)

                binding.metaText.text = listOfNotNull(
                    releaseYear,
                    info?.genre,
                    info?.rating?.let { if (it != "0") "$it ⭐" else null }
                ).joinToString(" · ")

                binding.plotText.text = info?.plot ?: ""

                val progress = app.repository.getProgress(streamId.toString())
                binding.playButton.text = if (progress != null && progress.positionMs > 5000) {
                    getString(R.string.resume)
                } else {
                    getString(R.string.play_from_start)
                }
            }.onFailure {
                binding.plotText.text = it.message ?: "Film bilgisi alınamadı"
            }

            // TMDB zenginleştirme (opsiyonel — sadece Ayarlar'da anahtar tanımlıysa)
            val tmdbKey = app.prefsManager.getTmdbApiKey()
            if (tmdbKey != null) {
                val enriched = app.tmdbRepository.enrichMovie(tmdbKey, title, releaseYear)
                if (enriched != null) applyTmdbEnrichment(enriched)
            }
        }

        binding.playButton.setOnClickListener { playMovie(title) }
    }

    private fun applyTmdbEnrichment(info: com.napxstream.data.model.TmdbEnrichedInfo) {
        // Afiş/backdrop: TMDB'nin görseli genelde Xtream'inkinden daha kaliteli/tutarlıdır
        if (!info.backdropUrl.isNullOrBlank()) {
            posterUrl = info.backdropUrl
            Glide.with(this).load(info.backdropUrl).into(binding.backdropImage)
        }

        // Konu metni boşsa TMDB'ninkiyle doldur
        if (binding.plotText.text.isNullOrBlank() && info.overview.isNotBlank()) {
            binding.plotText.text = info.overview
        }

        // Puan/tür bilgisini TMDB ile tamamla (Xtream'de eksikse)
        if (binding.metaText.text.isNullOrBlank()) {
            binding.metaText.text = listOfNotNull(
                info.year.ifBlank { null },
                info.genres.ifBlank { null },
                if (info.rating > 0) "${"%.1f".format(info.rating)} ⭐" else null
            ).joinToString(" · ")
        }

        if (info.cast.isNotEmpty()) {
            binding.castLabel.visibility = View.VISIBLE
            binding.castText.visibility = View.VISIBLE
            binding.castText.text = info.cast.joinToString(", ")
        }

        if (!info.trailerUrl.isNullOrBlank()) {
            trailerUrl = info.trailerUrl
            binding.trailerButton.visibility = View.VISIBLE
            binding.trailerButton.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.trailerUrl)))
            }
        }
    }

    private fun playMovie(title: String) {
        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return
        val url = account.vodStreamUrl(streamId, containerExtension)

        startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(Constants.EXTRA_STREAM_URL, url)
            putExtra(Constants.EXTRA_TITLE, title)
            putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_VOD)
            putExtra(Constants.EXTRA_CONTENT_ID, streamId.toString())
            putExtra(Constants.EXTRA_POSTER_URL, posterUrl)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
