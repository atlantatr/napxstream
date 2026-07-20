package com.napxstream.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napxstream.data.api.XtreamAccount
import com.napxstream.data.model.LiveStream
import com.napxstream.data.model.SeriesItem
import com.napxstream.data.model.VodStream
import com.napxstream.data.repository.XtreamRepository
import com.napxstream.util.Constants
import com.napxstream.util.Resource
import kotlinx.coroutines.launch

data class SearchResult(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val type: String // Constants.CONTENT_TYPE_*
)

class SearchViewModel(private val repository: XtreamRepository) : ViewModel() {

    private var allLive: List<LiveStream> = emptyList()
    private var allVod: List<VodStream> = emptyList()
    private var allSeries: List<SeriesItem> = emptyList()
    private var loaded = false

    private val _results = MutableLiveData<Resource<List<SearchResult>>>()
    val results: LiveData<Resource<List<SearchResult>>> = _results

    fun ensureLoaded(account: XtreamAccount) {
        if (loaded) return
        viewModelScope.launch {
            repository.getLiveStreams(account).onSuccess { allLive = it }
            repository.getVodStreams(account).onSuccess { allVod = it }
            repository.getSeriesList(account).onSuccess { allSeries = it }
            loaded = true
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _results.value = Resource.Success(emptyList())
            return
        }
        val q = query.trim().lowercase()
        val liveResults = allLive.filter { it.name?.lowercase()?.contains(q) == true }
            .let { com.napxstream.util.ParentalControlManager.filterLiveStreams(it) }
            .map { SearchResult(it.streamId, it.name ?: "-", it.streamIcon, Constants.CONTENT_TYPE_LIVE) }
        val vodResults = allVod.filter { it.name?.lowercase()?.contains(q) == true }
            .map { SearchResult(it.streamId, it.name ?: "-", it.streamIcon, Constants.CONTENT_TYPE_VOD) }
        val seriesResults = allSeries.filter { it.name?.lowercase()?.contains(q) == true }
            .map { SearchResult(it.seriesId, it.name ?: "-", it.cover, Constants.CONTENT_TYPE_SERIES) }

        _results.value = Resource.Success(liveResults + vodResults + seriesResults)
    }
}
