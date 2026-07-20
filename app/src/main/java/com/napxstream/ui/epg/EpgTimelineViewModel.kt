package com.napxstream.ui.epg

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napxstream.data.api.XtreamAccount
import com.napxstream.data.repository.XtreamRepository
import com.napxstream.util.Resource
import kotlinx.coroutines.launch

class EpgTimelineViewModel(private val repository: XtreamRepository) : ViewModel() {

    private val _rows = MutableLiveData<Resource<List<EpgChannelRow>>>()
    val rows: LiveData<Resource<List<EpgChannelRow>>> = _rows

    fun load(account: XtreamAccount, channelIds: List<Int>, channelNames: List<String>, archiveFlags: List<Boolean> = emptyList()) {
        _rows.value = Resource.Loading
        viewModelScope.launch {
            try {
                val epgMap = repository.getShortEpgForChannels(account, channelIds, limit = 8)
                val result = channelIds.mapIndexed { index, id ->
                    EpgChannelRow(
                        streamId = id,
                        name = channelNames.getOrElse(index) { "-" },
                        programs = epgMap[id] ?: emptyList(),
                        tvArchive = archiveFlags.getOrElse(index) { false }
                    )
                }
                _rows.value = Resource.Success(result)
            } catch (e: Exception) {
                _rows.value = Resource.Error(e.message ?: "EPG verisi alınamadı")
            }
        }
    }
}
