package com.napxstream.ui.series

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napxstream.data.api.XtreamAccount
import com.napxstream.data.model.Category
import com.napxstream.data.model.SeriesItem
import com.napxstream.data.repository.XtreamRepository
import com.napxstream.util.Resource
import kotlinx.coroutines.launch

class SeriesViewModel(private val repository: XtreamRepository) : ViewModel() {

    private val _categories = MutableLiveData<Resource<List<Category>>>()
    val categories: LiveData<Resource<List<Category>>> = _categories

    private val _series = MutableLiveData<Resource<List<SeriesItem>>>()
    val series: LiveData<Resource<List<SeriesItem>>> = _series

    var selectedCategoryId: String? = null
        private set

    fun loadCategories(account: XtreamAccount) {
        _categories.value = Resource.Loading
        viewModelScope.launch {
            repository.getSeriesCategories(account)
                .onSuccess { _categories.value = Resource.Success(com.napxstream.util.ParentalControlManager.filterCategories(it)) }
                .onFailure { _categories.value = Resource.Error(it.message ?: "Hata") }
        }
    }

    fun loadSeries(account: XtreamAccount, categoryId: String?) {
        selectedCategoryId = categoryId
        _series.value = Resource.Loading
        viewModelScope.launch {
            repository.getSeriesList(account, categoryId)
                .onSuccess { _series.value = Resource.Success(it) }
                .onFailure { _series.value = Resource.Error(it.message ?: "Hata") }
        }
    }
}
