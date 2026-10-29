package com.kazatune.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.comments.YouTubeComment
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentViewModel(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _comments = MutableStateFlow<List<YouTubeComment>>(emptyList())
    val comments: StateFlow<List<YouTubeComment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadComments(videoId: String) {
        if (videoId.isBlank()) return
        _isLoading.value = true
        _comments.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                songRepository.getComments(videoId).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _comments.value = resource.data?.comments ?: emptyList()
                            _isLoading.value = false
                        }
                        is Resource.Error -> {
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}
