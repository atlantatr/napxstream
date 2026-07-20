package com.napxstream.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.napxstream.XtreamApp
import com.napxstream.data.model.Category
import com.napxstream.data.model.Episode
import com.napxstream.databinding.ActivitySeriesDetailBinding
import com.napxstream.ui.main.CategoryAdapter
import com.napxstream.ui.player.PlayerActivity
import com.napxstream.util.Constants
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

/**
 * SeriesDetailActivity ile aynı iş mantığını taşır; hem tam ekran Activity içinde
 * (telefon) hem de tablet master-detail panelinde gömülü olarak kullanılabilmesi
 * için Fragment olarak çıkarılmıştır (bkz. MovieDetailFragment ile aynı desen).
 */
class SeriesDetailFragment : Fragment() {

    private var _binding: ActivitySeriesDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var seasonAdapter: CategoryAdapter
    private lateinit var episodeAdapter: EpisodeAdapter

    private var seriesId: Int = -1
    private var seriesName: String = ""
    private var seriesCoverUrl: String? = null
    private var episodesBySeason: Map<String, List<Episode>> = emptyMap()

    /** Gömülü (tablet) modda geri butonuna basıldığında çağrılır; host Activity yoksa gizlenir. */
    var onCloseRequested: (() -> Unit)? = null
        get() = field ?: (activity as? SeriesDetailActivity)?.let { { it.finish() } }

    companion object {
        fun newInstance(seriesId: Int, title: String): SeriesDetailFragment {
            return SeriesDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.EXTRA_SERIES_ID, seriesId)
                    putString(Constants.EXTRA_TITLE, title)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivitySeriesDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seriesId = arguments?.getInt(Constants.EXTRA_SERIES_ID) ?: -1
        seriesName = arguments?.getString(Constants.EXTRA_TITLE) ?: ""
        binding.titleText.text = seriesName

        if (onCloseRequested != null) {
            binding.backButton.setOnClickListener { onCloseRequested?.invoke() }
        } else {
            binding.backButton.visibility = View.GONE
        }

        seasonAdapter = CategoryAdapter { category ->
            seasonAdapter.setSelected(category.categoryId)
            showEpisodes(category.categoryId)
        }
        binding.seasonRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.seasonRecycler.adapter = seasonAdapter

        episodeAdapter = EpisodeAdapter { episode -> playEpisode(episode) }
        binding.episodeRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.episodeRecycler.adapter = episodeAdapter

        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            binding.loadingProgress.visibility = View.VISIBLE
            val result = app.repository.getSeriesInfo(account, seriesId)
            binding.loadingProgress.visibility = View.GONE

            result.onSuccess { response ->
                seriesCoverUrl = response.info?.cover
                Glide.with(this@SeriesDetailFragment)
                    .load(response.info?.cover)
                    .into(binding.coverImage)
                binding.plotText.text = response.info?.plot ?: ""

                episodesBySeason = response.episodes ?: emptyMap()
                val seasonCategories = episodesBySeason.keys
                    .sortedBy { it.toIntOrNull() ?: 0 }
                    .map { Category(categoryId = it, categoryName = "Sezon $it", parentId = null) }
                seasonAdapter.submitList(seasonCategories)

                val firstSeason = seasonCategories.firstOrNull()
                if (firstSeason != null) {
                    seasonAdapter.setSelected(firstSeason.categoryId)
                    showEpisodes(firstSeason.categoryId)
                }
            }.onFailure {
                binding.plotText.text = it.message ?: "Dizi bilgisi alınamadı"
            }

            // TMDB zenginleştirme (opsiyonel — sadece Ayarlar'da anahtar tanımlıysa)
            val tmdbKey = app.prefsManager.getTmdbApiKey()
            if (tmdbKey != null) {
                val enriched = app.tmdbRepository.enrichSeries(tmdbKey, seriesName)
                if (enriched != null) applyTmdbEnrichment(enriched)
            }
        }
    }

    private fun applyTmdbEnrichment(info: com.napxstream.data.model.TmdbEnrichedInfo) {
        if (!info.backdropUrl.isNullOrBlank()) {
            seriesCoverUrl = info.backdropUrl
            Glide.with(this).load(info.backdropUrl).into(binding.coverImage)
        }
        if (binding.plotText.text.isNullOrBlank() && info.overview.isNotBlank()) {
            binding.plotText.text = info.overview
        }
        if (info.cast.isNotEmpty()) {
            binding.castText.visibility = View.VISIBLE
            binding.castText.text = getString(com.napxstream.R.string.cast) + ": " + info.cast.joinToString(", ")
        }
        if (!info.trailerUrl.isNullOrBlank()) {
            binding.trailerButton.visibility = View.VISIBLE
            binding.trailerButton.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(info.trailerUrl)))
            }
        }
    }

    private fun showEpisodes(season: String) {
        episodeAdapter.submitList(episodesBySeason[season] ?: emptyList())
    }

    private fun playEpisode(episode: Episode) {
        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return
        val extension = episode.containerExtension ?: "mp4"
        val url = account.seriesStreamUrl(episode.id, extension)
        val episodeTitle = "$seriesName - ${episode.title ?: "Bölüm ${episode.episodeNum}"}"

        // Aynı sezon içinde bu bölümden sonraki bölümü bul (sıradaki bölüm otomatik oynatma için)
        val seasonKey = episode.season?.toString()
        val seasonEpisodes = episodesBySeason[seasonKey] ?: emptyList()
        val currentIndex = seasonEpisodes.indexOfFirst { it.id == episode.id }
        val nextEpisode = if (currentIndex in seasonEpisodes.indices) seasonEpisodes.getOrNull(currentIndex + 1) else null

        startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(Constants.EXTRA_STREAM_URL, url)
            putExtra(Constants.EXTRA_TITLE, episodeTitle)
            putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_SERIES)
            putExtra(Constants.EXTRA_CONTENT_ID, episode.id)
            putExtra(Constants.EXTRA_PARENT_ID, seriesId.toString())
            putExtra(Constants.EXTRA_POSTER_URL, seriesCoverUrl)

            if (nextEpisode != null) {
                val nextExt = nextEpisode.containerExtension ?: "mp4"
                putExtra(Constants.EXTRA_NEXT_STREAM_URL, account.seriesStreamUrl(nextEpisode.id, nextExt))
                putExtra(
                    Constants.EXTRA_NEXT_TITLE,
                    "$seriesName - ${nextEpisode.title ?: "Bölüm ${nextEpisode.episodeNum}"}"
                )
                putExtra(Constants.EXTRA_NEXT_CONTENT_ID, nextEpisode.id)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
