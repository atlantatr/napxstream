package com.napxstream.ui.live

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napxstream.data.api.XtreamAccount
import com.napxstream.data.local.FavoriteEntity
import com.napxstream.data.model.Category
import com.napxstream.data.model.EpgListing
import com.napxstream.data.model.LiveStream
import com.napxstream.data.repository.XtreamRepository
import com.napxstream.util.Constants
import com.napxstream.util.Resource
import kotlinx.coroutines.launch

class LiveViewModel(private val repository: XtreamRepository) : ViewModel() {

    private val _categories = MutableLiveData<Resource<List<Category>>>()
    val categories: LiveData<Resource<List<Category>>> = _categories

    private val _channels = MutableLiveData<Resource<List<LiveStream>>>()
    val channels: LiveData<Resource<List<LiveStream>>> = _channels

    private val _nowPlaying = MutableLiveData<Pair<Int, String>>() // streamId to text
    val nowPlaying: LiveData<Pair<Int, String>> = _nowPlaying

    var selectedCategoryId: String? = null
        private set

    fun loadCategories(account: XtreamAccount) {
        _categories.value = Resource.Loading
        viewModelScope.launch {
            repository.getLiveCategories(account)
                .onSuccess { _categories.value = Resource.Success(com.napxstream.util.ParentalControlManager.filterCategories(it)) }
                .onFailure { _categories.value = Resource.Error(it.message ?: "Hata") }
        }
    }

    fun loadChannels(account: XtreamAccount, categoryId: String?) {
        selectedCategoryId = categoryId
        _channels.value = Resource.Loading
        viewModelScope.launch {
            repository.getLiveStreams(account, categoryId)
                .onSuccess { _channels.value = Resource.Success(com.napxstream.util.ParentalControlManager.filterLiveStreams(it)) }
                .onFailure { _channels.value = Resource.Error(it.message ?: "Hata") }
        }
    }

    fun loadNowPlaying(account: XtreamAccount, streamId: Int) {
        viewModelScope.launch {
            repository.getShortEpg(account, streamId, limit = 1).onSuccess { response ->
                val current: EpgListing? = response.epgListings?.firstOrNull()
                val title = Constants.decodeEpgText(current?.title)
                if (title.isNotBlank()) {
                    _nowPlaying.value = streamId to title
                }
            }
        }
    }

    fun isFavorite(streamId: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.isFavorite(streamId, Constants.CONTENT_TYPE_LIVE))
        }
    }

    fun toggleFavorite(channel: LiveStream, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(
                FavoriteEntity(
                    streamId = channel.streamId,
                    type = Constants.CONTENT_TYPE_LIVE,
                    name = channel.name ?: "",
                    iconUrl = channel.streamIcon,
                    categoryId = channel.categoryId
                ),
                currentlyFavorite
            )
        }
    }
}
