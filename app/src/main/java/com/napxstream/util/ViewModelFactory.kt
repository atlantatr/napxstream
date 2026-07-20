package com.napxstream.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.napxstream.data.repository.XtreamRepository

/**
 * Tek repository parametresi alan tüm ViewModel'lar için genel fabrika.
 * Constructor referansı verilerek kullanılır, örn:
 * ViewModelFactory(repo) { LoginViewModel(it) }
 */
class ViewModelFactory(
    private val repository: XtreamRepository,
    private val create: (XtreamRepository) -> ViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return create(repository) as T
    }
}
