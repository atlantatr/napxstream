package com.napxstream.ui.series

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.napxstream.XtreamApp
import com.napxstream.databinding.FragmentSeriesMasterDetailBinding
import com.napxstream.ui.main.CategoryAdapter
import com.napxstream.ui.main.ContinueWatchingAdapter
import com.napxstream.ui.main.PosterAdapter
import com.napxstream.ui.main.PosterItem
import com.napxstream.util.Constants
import com.napxstream.util.Resource
import com.napxstream.util.ViewModelFactory

/**
 * Geniş ekranlar (tablet, sw600dp+) için master-detail Diziler sürümü.
 * Aynı desen VodMasterDetailFragment ile birebir aynıdır.
 */
class SeriesMasterDetailFragment : Fragment() {

    private var _binding: FragmentSeriesMasterDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SeriesViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var posterAdapter: PosterAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesMasterDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return

        viewModel = ViewModelProvider(this, ViewModelFactory(app.repository) { SeriesViewModel(it) })[SeriesViewModel::class.java]

        categoryAdapter = CategoryAdapter { category ->
            categoryAdapter.setSelected(category.categoryId)
            viewModel.loadSeries(account, category.categoryId)
        }
        binding.categoryRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoryRecycler.adapter = categoryAdapter

        val continueWatchingAdapter = ContinueWatchingAdapter { item ->
            val sId = item.parentId?.toIntOrNull() ?: return@ContinueWatchingAdapter
            showDetailInPane(PosterItem(sId, item.title.substringBefore(" - "), item.posterUrl))
        }
        binding.continueWatchingRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.continueWatchingRecycler.adapter = continueWatchingAdapter

        app.repository.getContinueWatchingByType(Constants.CONTENT_TYPE_SERIES).observe(viewLifecycleOwner) { items ->
            binding.continueWatchingSection.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            continueWatchingAdapter.submitList(items)
        }

        val spanCount = (resources.configuration.screenWidthDp / 2 / 120).coerceAtLeast(2)
        posterAdapter = PosterAdapter { item -> showDetailInPane(item) }
        binding.posterRecycler.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.posterRecycler.adapter = posterAdapter

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val pane = binding.slidingPane
            if (pane.isSlideable && pane.isOpen) {
                pane.closePane()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadSeries(account, viewModel.selectedCategoryId)
        }

        viewModel.categories.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                categoryAdapter.submitList(resource.data)
                val first = resource.data.firstOrNull()
                if (first != null && viewModel.selectedCategoryId == null) {
                    categoryAdapter.setSelected(first.categoryId)
                    viewModel.loadSeries(account, first.categoryId)
                }
            }
        }

        viewModel.series.observe(viewLifecycleOwner) { resource ->
            binding.swipeRefresh.isRefreshing = false
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingProgress.visibility = View.VISIBLE
                    binding.emptyText.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.loadingProgress.visibility = View.GONE
                    val posterItems = resource.data.map {
                        PosterItem(it.seriesId, it.name ?: "-", it.cover, it.rating)
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

    private fun showDetailInPane(item: PosterItem) {
        binding.detailPlaceholder.visibility = View.GONE
        val fragment = SeriesDetailFragment.newInstance(item.id, item.title)
        childFragmentManager.beginTransaction()
            .replace(binding.detailContainer.id, fragment)
            .commit()
        binding.slidingPane.openPane()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
