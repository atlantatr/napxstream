package com.napxstream.ui.m3u

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.napxstream.XtreamApp
import com.napxstream.data.local.FavoriteEntity
import com.napxstream.data.model.Category
import com.napxstream.data.model.M3uEntry
import com.napxstream.databinding.FragmentM3uBrowseBinding
import com.napxstream.ui.main.CategoryAdapter
import com.napxstream.ui.main.M3uChannelAdapter
import com.napxstream.ui.main.PosterAdapter
import com.napxstream.ui.main.PosterItem
import com.napxstream.ui.player.PlayerActivity
import com.napxstream.util.Constants
import kotlinx.coroutines.launch

/**
 * M3U kaynağı için Canlı/Film/Dizi sekmelerinin üçünde de kullanılan ortak ekran.
 * Xtream'deki ayrı kategori/stream API'leri yerine, tüm playlist tek seferde
 * M3uRepository üzerinden (Room önbelleğinden) okunur ve group-title'a göre
 * kategorilere ayrılır.
 */
class M3uBrowseFragment : Fragment() {

    private var _binding: FragmentM3uBrowseBinding? = null
    private val binding get() = _binding!!

    private lateinit var contentType: String
    private lateinit var categoryAdapter: CategoryAdapter
    private var channelAdapter: M3uChannelAdapter? = null
    private var posterAdapter: PosterAdapter? = null
    private var allEntries: List<M3uEntry> = emptyList()
    private var favoriteIds: Set<String> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentM3uBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentType = arguments?.getString(ARG_CONTENT_TYPE) ?: Constants.CONTENT_TYPE_LIVE

        val app = requireActivity().application as XtreamApp
        val playlistUrl = app.prefsManager.getM3uUrl() ?: return

        categoryAdapter = CategoryAdapter { category ->
            categoryAdapter.setSelected(category.categoryId)
            renderEntries(if (category.categoryId == ALL_CATEGORY_ID) null else category.categoryId)
        }
        binding.categoryRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoryRecycler.adapter = categoryAdapter

        if (contentType == Constants.CONTENT_TYPE_LIVE) {
            val adapter = M3uChannelAdapter(
                onClick = { entry -> playLive(entry) },
                onFavoriteClick = { entry -> toggleFavorite(app, entry) },
                isFavorite = { favoriteIds.contains(it) }
            )
            channelAdapter = adapter
            binding.contentRecycler.layoutManager = LinearLayoutManager(requireContext())
            binding.contentRecycler.adapter = adapter
        } else {
            val spanCount = (resources.configuration.screenWidthDp / 120).coerceAtLeast(2)
            val adapter = PosterAdapter { item -> openDetail(item) }
            posterAdapter = adapter
            binding.contentRecycler.layoutManager = GridLayoutManager(requireContext(), spanCount)
            binding.contentRecycler.adapter = adapter
        }

        binding.swipeRefresh.setOnRefreshListener { loadPlaylist(app, playlistUrl, forceRefresh = true) }

        app.repository.getFavorites(contentType).observe(viewLifecycleOwner) { favorites ->
            favoriteIds = favorites.filter { it.isM3u }.mapNotNull { it.m3uEntryId }.toSet()
            channelAdapter?.notifyDataSetChanged()
        }

        loadPlaylist(app, playlistUrl, forceRefresh = false)
    }

    private fun loadPlaylist(app: XtreamApp, playlistUrl: String, forceRefresh: Boolean) {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.emptyText.visibility = View.GONE
        lifecycleScope.launch {
            val result = app.m3uRepository.ensureLoaded(playlistUrl, forceRefresh)
            binding.swipeRefresh.isRefreshing = false
            result.onSuccess {
                allEntries = app.m3uRepository.getEntries(playlistUrl, contentType, group = null)
                val groups = allEntries.map { it.groupTitle }.distinct().sorted()
                val categories = listOf(Category(ALL_CATEGORY_ID, getString(com.napxstream.R.string.all_categories), null)) +
                    groups.map { Category(it, it, null) }
                categoryAdapter.submitList(categories)
                categoryAdapter.setSelected(ALL_CATEGORY_ID)
                renderEntries(null)
            }.onFailure {
                binding.loadingProgress.visibility = View.GONE
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = it.message ?: "Playlist yüklenemedi"
            }
        }
    }

    private fun renderEntries(group: String?) {
        binding.loadingProgress.visibility = View.GONE
        val filtered = if (group == null) allEntries else allEntries.filter { it.groupTitle == group }
        binding.emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        if (contentType == Constants.CONTENT_TYPE_LIVE) {
            channelAdapter?.submitList(filtered)
        } else {
            posterAdapter?.submitList(filtered.map { PosterItem(it.entryId.hashCode(), it.name, it.logoUrl) })
        }
    }

    private fun playLive(entry: M3uEntry) {
        startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(Constants.EXTRA_STREAM_URL, entry.streamUrl)
            putExtra(Constants.EXTRA_TITLE, entry.name)
            putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_LIVE)
        })
    }

    private fun openDetail(item: PosterItem) {
        val entry = allEntries.firstOrNull { it.entryId.hashCode() == item.id } ?: return
        startActivity(Intent(requireContext(), M3uDetailActivity::class.java).apply {
            putExtra(Constants.EXTRA_M3U_ENTRY_ID, entry.entryId)
            putExtra(Constants.EXTRA_TITLE, entry.name)
            putExtra(Constants.EXTRA_M3U_CONTENT_TYPE, entry.contentType)
        })
    }

    private fun toggleFavorite(app: XtreamApp, entry: M3uEntry) {
        val isFav = favoriteIds.contains(entry.entryId)
        lifecycleScope.launch {
            app.repository.toggleFavorite(
                FavoriteEntity(
                    streamId = entry.entryId.hashCode(),
                    type = contentType,
                    name = entry.name,
                    iconUrl = entry.logoUrl,
                    categoryId = entry.groupTitle,
                    isM3u = true,
                    streamUrl = entry.streamUrl,
                    m3uEntryId = entry.entryId
                ),
                isFav
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CONTENT_TYPE = "content_type"
        const val ALL_CATEGORY_ID = "__all__"

        fun newInstance(contentType: String): M3uBrowseFragment = M3uBrowseFragment().apply {
            arguments = Bundle().apply { putString(ARG_CONTENT_TYPE, contentType) }
        }
    }
}
