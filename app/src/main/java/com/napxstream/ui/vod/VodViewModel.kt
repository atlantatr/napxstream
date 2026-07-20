package com.napxstream.ui.vod

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napxstream.data.api.XtreamAccount
import com.napxstream.data.model.Category
import com.napxstream.data.model.VodStream
import com.napxstream.data.repository.XtreamRepository
import com.napxstream.util.Resource
import kotlinx.coroutines.launch

class VodViewModel(private val repository: XtreamRepository) : ViewModel() {

    private val _categories = MutableLiveData<Resource<List<Category>>>()
    val categories: LiveData<Resource<List<Category>>> = _categories

    private val _movies = MutableLiveData<Resource<List<VodStream>>>()
    val movies: LiveData<Resource<List<VodStream>>> = _movies

    var selectedCategoryId: String? = null
        private set

    fun loadCategories(account: XtreamAccount) {
        _categories.value = Resource.Loading
        viewModelScope.launch {
            repository.getVodCategories(account)
                .onSuccess { _categories.value = Resource.Success(com.napxstream.util.ParentalControlManager.filterCategories(it)) }
                .onFailure { _categories.value = Resource.Error(it.message ?: "Hata") }
        }
    }

    fun loadMovies(account: XtreamAccount, categoryId: String?) {
        selectedCategoryId = categoryId
        _movies.value = Resource.Loading
        viewModelScope.launch {
            repository.getVodStreams(account, categoryId)
                .onSuccess { _movies.value = Resource.Success(it) }
                .onFailure { _movies.value = Resource.Error(it.message ?: "Hata") }
        }
    }
}
