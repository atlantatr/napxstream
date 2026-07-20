package com.napxstream.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.napxstream.XtreamApp
import com.napxstream.data.local.FavoriteEntity
import com.napxstream.databinding.FragmentFavoritesBinding
import com.napxstream.ui.main.PosterAdapter
import com.napxstream.ui.main.PosterItem
import com.napxstream.ui.player.PlayerActivity
import com.napxstream.ui.series.SeriesDetailActivity
import com.napxstream.ui.vod.MovieDetailActivity
import com.napxstream.util.Constants
import com.google.android.material.tabs.TabLayout

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var posterAdapter: PosterAdapter
    private var currentType = Constants.CONTENT_TYPE_LIVE
    private var currentFavorites: List<FavoriteEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as XtreamApp

        posterAdapter = PosterAdapter { item -> openItem(item) }
        val spanCount = (resources.configuration.screenWidthDp / 120).coerceAtLeast(2)
        binding.favoriteRecycler.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.favoriteRecycler.adapter = posterAdapter

        observeType(app, currentType)

        binding.favTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentType = when (tab?.position) {
                    1 -> Constants.CONTENT_TYPE_VOD
                    2 -> Constants.CONTENT_TYPE_SERIES
                    else -> Constants.CONTENT_TYPE_LIVE
                }
                observeType(app, currentType)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeType(app: XtreamApp, type: String) {
        app.repository.getFavorites(type).observe(viewLifecycleOwner) { favorites ->
            currentFavorites = favorites
            val posterItems = favorites.map { PosterItem(it.streamId, it.name, it.iconUrl) }
            posterAdapter.submitList(posterItems)
            binding.emptyText.visibility = if (posterItems.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openItem(item: PosterItem) {
        val app = requireActivity().application as XtreamApp
        val favorite = currentFavorites.firstOrNull { it.streamId == item.id } ?: return

        if (favorite.isM3u) {
            openM3uFavorite(favorite)
            return
        }

        val account = app.prefsManager.getAccount() ?: return
        when (currentType) {
            Constants.CONTENT_TYPE_LIVE -> {
                val url = account.liveStreamUrl(favorite.streamId)
                startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(Constants.EXTRA_STREAM_URL, url)
                    putExtra(Constants.EXTRA_TITLE, favorite.name)
                    putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_LIVE)
                    putExtra(Constants.EXTRA_STREAM_ID, favorite.streamId)
                })
            }
            Constants.CONTENT_TYPE_VOD -> {
                startActivity(Intent(requireContext(), MovieDetailActivity::class.java).apply {
                    putExtra(Constants.EXTRA_STREAM_ID, favorite.streamId)
                    putExtra(Constants.EXTRA_TITLE, favorite.name)
                })
            }
            Constants.CONTENT_TYPE_SERIES -> {
                startActivity(Intent(requireContext(), SeriesDetailActivity::class.java).apply {
                    putExtra(Constants.EXTRA_SERIES_ID, favorite.streamId)
                    putExtra(Constants.EXTRA_TITLE, favorite.name)
                })
            }
        }
    }

    private fun openM3uFavorite(favorite: FavoriteEntity) {
        if (currentType == Constants.CONTENT_TYPE_LIVE) {
            val url = favorite.streamUrl ?: return
            startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra(Constants.EXTRA_STREAM_URL, url)
                putExtra(Constants.EXTRA_TITLE, favorite.name)
                putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_LIVE)
            })
        } else {
            val entryId = favorite.m3uEntryId ?: return
            startActivity(Intent(requireContext(), com.napxstream.ui.m3u.M3uDetailActivity::class.java).apply {
                putExtra(Constants.EXTRA_M3U_ENTRY_ID, entryId)
                putExtra(Constants.EXTRA_TITLE, favorite.name)
                putExtra(Constants.EXTRA_M3U_CONTENT_TYPE, currentType)
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
