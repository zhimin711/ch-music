package com.ch.music.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ch.music.network.Result
import com.ch.music.network.models.NeteaseSong
import com.ch.music.repository.NeteaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 网易云在线单曲搜索状态。 */
class OnlineSearchViewModel(
    private val neteaseRepository: NeteaseRepository
) : ViewModel() {

    private val _songs = MutableLiveData<Result<List<NeteaseSong>>>()
    val songs: LiveData<Result<List<NeteaseSong>>> = _songs

    private var searchJob: Job? = null

    fun search(keywords: String) {
        searchJob?.cancel()
        if (keywords.isBlank()) {
            _songs.value = Result.Success(emptyList())
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _songs.postValue(Result.Loading)
            _songs.postValue(neteaseRepository.searchSongs(keywords.trim()))
        }
    }
}
