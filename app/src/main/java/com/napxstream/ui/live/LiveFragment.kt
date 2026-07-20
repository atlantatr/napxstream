package com.napxstream.ui.live

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.napxstream.XtreamApp
import com.napxstream.data.model.Category
import com.napxstream.data.model.LiveStream
import com.napxstream.databinding.FragmentLiveBinding
import com.napxstream.ui.main.CategoryAdapter
import com.napxstream.ui.player.PlayerActivity
import com.napxstream.util.Constants
import com.napxstream.util.Resource
import com.napxstream.util.ViewModelFactory
import android.content.Intent

class LiveFragment : Fragment() {

    private var _binding: FragmentLiveBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LiveViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private var favoriteIds: Set<Int> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return

        viewModel = ViewModelProvider(this, ViewModelFactory(app.repository) { LiveViewModel(it) })[LiveViewModel::class.java]

        categoryAdapter = CategoryAdapter { category ->
            categoryAdapter.setSelected(category.categoryId)
            viewModel.loadChannels(account, category.categoryId)
        }
        binding.categoryRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoryRecycler.adapter = categoryAdapter

        channelAdapter = ChannelAdapter(
            onClick = { channel -> openPlayer(channel) },
            onFavoriteClick = { channel ->
                val isFav = favoriteIds.contains(channel.streamId)
                viewModel.toggleFavorite(channel, isFav)
            },
            isFavorite = { favoriteIds.contains(it) },
            onLongClick = { channel -> openEpg(channel) }
        )
        binding.channelRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.channelRecycler.adapter = channelAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadChannels(account, viewModel.selectedCategoryId)
        }

        app.repository.getFavorites(Constants.CONTENT_TYPE_LIVE).observe(viewLifecycleOwner) { favorites ->
            favoriteIds = favorites.map { it.streamId }.toSet()
            channelAdapter.notifyDataSetChanged()
        }

        viewModel.categories.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    categoryAdapter.submitList(resource.data)
                    val first = resource.data.firstOrNull()
                    if (first != null && viewModel.selectedCategoryId == null) {
                        categoryAdapter.setSelected(first.categoryId)
                        viewModel.loadChannels(account, first.categoryId)
                    }
                }
                is Resource.Error -> { /* sessizce yut, kanal listesi hatası ana hatadır */ }
                is Resource.Loading -> { }
            }
        }

        viewModel.channels.observe(viewLifecycleOwner) { resource ->
            binding.swipeRefresh.isRefreshing = false
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingProgress.visibility = View.VISIBLE
                    binding.emptyText.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.loadingProgress.visibility = View.GONE
                    channelAdapter.submitList(resource.data)
                    binding.emptyText.visibility = if (resource.data.isEmpty()) View.VISIBLE else View.GONE
                    resource.data.take(30).forEach { viewModel.loadNowPlaying(account, it.streamId) }
                }
                is Resource.Error -> {
                    binding.loadingProgress.visibility = View.GONE
                    binding.emptyText.visibility = View.VISIBLE
                    binding.emptyText.text = resource.message
                }
            }
        }

        viewModel.nowPlaying.observe(viewLifecycleOwner) { (streamId, text) ->
            channelAdapter.updateNowPlaying(streamId, getString(com.napxstream.R.string.now_playing) + ": " + text)
        }

        viewModel.loadCategories(account)
    }

    private fun openPlayer(channel: LiveStream) {
        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return
        val url = account.liveStreamUrl(channel.streamId)
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(Constants.EXTRA_STREAM_URL, url)
            putExtra(Constants.EXTRA_TITLE, channel.name)
            putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_LIVE)
            putExtra(Constants.EXTRA_STREAM_ID, channel.streamId)
        }
        startActivity(intent)
    }

    private fun openEpg(channel: LiveStream) {
        val allChannels = channelAdapter.currentList
        val focusIndex = allChannels.indexOfFirst { it.streamId == channel.streamId }.coerceAtLeast(0)

        // Çok kalabalık kategorilerde (yüzlerce kanal) her biri için eşzamanlı EPG isteği
        // atmamak adına, odaklanılan kanalın etrafında makul bir pencereye kırpıyoruz.
        val maxChannels = 40
        val windowed = if (allChannels.size > maxChannels) {
            val start = (focusIndex - 5).coerceIn(0, (allChannels.size - maxChannels).coerceAtLeast(0))
            allChannels.subList(start, (start + maxChannels).coerceAtMost(allChannels.size))
        } else {
            allChannels
        }

        val intent = Intent(requireContext(), com.napxstream.ui.epg.EpgTimelineActivity::class.java).apply {
            putExtra(Constants.EXTRA_CHANNEL_IDS, windowed.map { it.streamId }.toIntArray())
            putExtra(Constants.EXTRA_CHANNEL_NAMES, windowed.map { it.name ?: "-" }.toTypedArray())
            putExtra(Constants.EXTRA_CHANNEL_ARCHIVE_FLAGS, windowed.map { it.tvArchive == 1 }.toBooleanArray())
            putExtra(Constants.EXTRA_FOCUS_CHANNEL_ID, channel.streamId)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
