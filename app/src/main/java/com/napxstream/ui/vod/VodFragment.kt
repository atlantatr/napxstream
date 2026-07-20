package com.napxstream.ui.vod

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.napxstream.XtreamApp
import com.napxstream.databinding.FragmentVodBinding
import com.napxstream.ui.main.CategoryAdapter
import com.napxstream.ui.main.ContinueWatchingAdapter
import com.napxstream.ui.main.PosterAdapter
import com.napxstream.ui.main.PosterItem
import com.napxstream.util.Constants
import com.napxstream.util.Resource
import com.napxstream.util.ViewModelFactory

/**
 * Telefon/dar ekran sürümü: film seçildiğinde MovieDetailActivity'yi tam ekran açar.
 * Geniş ekranlarda (tablet, sw600dp+) bunun yerine VodMasterDetailFragment kullanılır
 * (bkz. MainActivity.kt - ekran genişliğine göre seçim yapılır).
 */
class VodFragment : Fragment() {

    private var _binding: FragmentVodBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: VodViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var posterAdapter: PosterAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return

        viewModel = ViewModelProvider(this, ViewModelFactory(app.repository) { VodViewModel(it) })[VodViewModel::class.java]

        categoryAdapter = CategoryAdapter { category ->
            categoryAdapter.setSelected(category.categoryId)
            viewModel.loadMovies(account, category.categoryId)
        }
        binding.categoryRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoryRecycler.adapter = categoryAdapter

        val continueWatchingAdapter = ContinueWatchingAdapter { item ->
            val id = item.contentId.toIntOrNull() ?: return@ContinueWatchingAdapter
            startActivity(Intent(requireContext(), MovieDetailActivity::class.java).apply {
                putExtra(Constants.EXTRA_STREAM_ID, id)
                putExtra(Constants.EXTRA_TITLE, item.title)
            })
        }
        binding.continueWatchingRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.continueWatchingRecycler.adapter = continueWatchingAdapter

        app.repository.getContinueWatchingByType(Constants.CONTENT_TYPE_VOD).observe(viewLifecycleOwner) { items ->
            binding.continueWatchingSection.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            continueWatchingAdapter.submitList(items)
        }

        val spanCount = resources.configuration.screenWidthDp / 120
        posterAdapter = PosterAdapter { item ->
            val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
                putExtra(Constants.EXTRA_STREAM_ID, item.id)
                putExtra(Constants.EXTRA_TITLE, item.title)
            }
            startActivity(intent)
        }
        binding.posterRecycler.layoutManager = GridLayoutManager(requireContext(), spanCount.coerceAtLeast(2))
        binding.posterRecycler.adapter = posterAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMovies(account, viewModel.selectedCategoryId)
        }

        viewModel.categories.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                categoryAdapter.submitList(resource.data)
                val first = resource.data.firstOrNull()
                if (first != null && viewModel.selectedCategoryId == null) {
                    categoryAdapter.setSelected(first.categoryId)
                    viewModel.loadMovies(account, first.categoryId)
                }
            }
        }

        viewModel.movies.observe(viewLifecycleOwner) { resource ->
            binding.swipeRefresh.isRefreshing = false
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingProgress.visibility = View.VISIBLE
                    binding.emptyText.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.loadingProgress.visibility = View.GONE
                    val posterItems = resource.data.map {
                        PosterItem(it.streamId, it.name ?: "-", it.streamIcon, it.rating)
                    }
                    posterAdapter.submitList(posterItems)
                    binding.emptyText.visibility = if (posterItems.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.loadingProgress.visibility = View.GONE
                    binding.emptyText.visibility = View.VISIBLE
                    binding.emptyText.text = resource.message
                }
            }
        }

        viewModel.loadCategories(account)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
