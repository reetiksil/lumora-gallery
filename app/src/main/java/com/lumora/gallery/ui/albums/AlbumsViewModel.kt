package com.lumora.gallery.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lumora.gallery.data.model.Album
import com.lumora.gallery.data.preferences.PreferencesRepository
import com.lumora.gallery.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumsUiState(
    val pinnedAlbums: List<Album> = emptyList(),
    val otherAlbums: List<Album> = emptyList(),
    val selectedInPhotosIds: Set<String> = emptySet(),
    val isLoading: Boolean = false
)

class AlbumsViewModel(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _allAlbums = MutableStateFlow<List<Album>>(emptyList())
    val allAlbums: StateFlow<List<Album>> = _allAlbums.asStateFlow()

    private val selectedBucketIds = preferencesRepository.selectedBucketIdsFlow
    private val pinnedBucketIds = preferencesRepository.pinnedBucketIdsFlow

    val uiState: StateFlow<AlbumsUiState> = combine(
        _allAlbums,
        selectedBucketIds,
        pinnedBucketIds
    ) { albums, selectedInPhotos, pinnedIds ->
        val effectivePinned = if (pinnedIds.isEmpty()) {
            // Default pinned albums: Camera, Screenshots, Videos if available
            albums.filter { it.isCamera || it.bucketName.contains("Screenshot", ignoreCase = true) }
        } else {
            albums.filter { pinnedIds.contains(it.bucketId) }
        }

        val other = albums.filterNot { effectivePinned.contains(it) }

        AlbumsUiState(
            pinnedAlbums = effectivePinned,
            otherAlbums = other,
            selectedInPhotosIds = selectedInPhotos,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumsUiState(isLoading = true))

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _allAlbums.value = mediaRepository.getAllAlbums()
        }
    }

    fun toggleShowInPhotos(bucketId: String) {
        viewModelScope.launch {
            val current = uiState.value.selectedInPhotosIds
            val show = !current.contains(bucketId)
            preferencesRepository.toggleBucketInPhotos(bucketId, show)
        }
    }

    fun togglePinAlbum(bucketId: String) {
        viewModelScope.launch {
            preferencesRepository.togglePinnedBucket(bucketId)
        }
    }

    companion object {
        fun provideFactory(
            mediaRepository: MediaRepository,
            preferencesRepository: PreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AlbumsViewModel(mediaRepository, preferencesRepository) as T
            }
        }
    }
}
