package com.napxstream.ui.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.napxstream.XtreamApp
import com.napxstream.databinding.FragmentSearchBinding
import com.napxstream.ui.main.PosterAdapter
import com.napxstream.ui.main.PosterItem
import com.napxstream.ui.player.PlayerActivity
import com.napxstream.ui.series.SeriesDetailActivity
import com.napxstream.ui.vod.MovieDetailActivity
import com.napxstream.util.Constants
import com.napxstream.util.Resource
import com.napxstream.util.ViewModelFactory

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: PosterAdapter
    private var currentResults: List<SearchResult> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return

        viewModel = ViewModelProvider(this, ViewModelFactory(app.repository) { SearchViewModel(it) })[SearchViewModel::class.java]

        adapter = PosterAdapter { item ->
            val result = currentResults.firstOrNull { it.id == item.id && it.title == item.title } ?: return@PosterAdapter
            openResult(result)
        }
        val spanCount = (resources.configuration.screenWidthDp / 120).coerceAtLeast(2)
        binding.searchRecycler.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.searchRecycler.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.results.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    currentResults = resource.data
                    adapter.submitList(resource.data.map { PosterItem(it.id, it.title, it.imageUrl) })
                    binding.emptyText.visibility =
                        if (resource.data.isEmpty() && binding.searchInput.text?.isNotBlank() == true) View.VISIBLE else View.GONE
                }
                else -> {}
            }
        }

        viewModel.ensureLoaded(account)
    }

    private fun openResult(result: SearchResult) {
        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount() ?: return
        when (result.type) {
            Constants.CONTENT_TYPE_LIVE -> {
                startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(Constants.EXTRA_STREAM_URL, account.liveStreamUrl(result.id))
                    putExtra(Constants.EXTRA_TITLE, result.title)
                    putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_LIVE)
                    putExtra(Constants.EXTRA_STREAM_ID, result.id)
                })
            }
            Constants.CONTENT_TYPE_VOD -> {
                startActivity(Intent(requireContext(), MovieDetailActivity::class.java).apply {
                    putExtra(Constants.EXTRA_STREAM_ID, result.id)
                    putExtra(Constants.EXTRA_TITLE, result.title)
                })
            }
            Constants.CONTENT_TYPE_SERIES -> {
                startActivity(Intent(requireContext(), SeriesDetailActivity::class.java).apply {
                    putExtra(Constants.EXTRA_SERIES_ID, result.id)
                    putExtra(Constants.EXTRA_TITLE, result.title)
                })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
