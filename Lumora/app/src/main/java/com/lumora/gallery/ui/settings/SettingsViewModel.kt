package com.lumora.gallery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lumora.gallery.data.model.Album
import com.lumora.gallery.data.model.SortOrder
import com.lumora.gallery.data.model.ThemeMode
import com.lumora.gallery.data.preferences.PreferencesRepository
import com.lumora.gallery.data.repository.MediaRepository
import com.lumora.gallery.data.util.MediaGrouping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedBucketIds: Set<String> = emptySet(),
    val includeNewAlbums: Boolean = false,
    val sortOrder: SortOrder = SortOrder.DATE_TAKEN_DESC,
    val diskCacheLimitMb: Int = 100,
    val currentCacheSizeBytes: Long = 0L,
    val currentCacheFormatted: String = "0 B",
    val autoplayMuted: Boolean = true,
    val showPrivateAlbum: Boolean = true,
    val eraseExifOnShare: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.AMOLED_DARK,
    val allAlbums: List<Album> = emptyList()
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            val albums = mediaRepository.getAllAlbums()
            val cacheBytes = mediaRepository.getDiskCacheSizeBytes()

            preferencesRepository.selectedBucketIdsFlow.collect { bucketIds ->
                _uiState.value = _uiState.value.copy(
                    selectedBucketIds = bucketIds,
                    allAlbums = albums,
                    currentCacheSizeBytes = cacheBytes,
                    currentCacheFormatted = MediaGrouping.formatStorageSize(cacheBytes)
                )
            }
        }

        viewModelScope.launch {
            preferencesRepository.includeNewAlbumsFlow.collect {
                _uiState.value = _uiState.value.copy(includeNewAlbums = it)
            }
        }

        viewModelScope.launch {
            preferencesRepository.sortOrderFlow.collect {
                _uiState.value = _uiState.value.copy(sortOrder = it)
            }
        }

        viewModelScope.launch {
            preferencesRepository.diskCacheLimitMbFlow.collect {
                _uiState.value = _uiState.value.copy(diskCacheLimitMb = it)
            }
        }

        viewModelScope.launch {
            preferencesRepository.autoplayVideosMutedFlow.collect {
                _uiState.value = _uiState.value.copy(autoplayMuted = it)
            }
        }

        viewModelScope.launch {
            preferencesRepository.showPrivateAlbumFlow.collect {
                _uiState.value = _uiState.value.copy(showPrivateAlbum = it)
            }
        }

        viewModelScope.launch {
            preferencesRepository.eraseExifOnShareFlow.collect {
                _uiState.value = _uiState.value.copy(eraseExifOnShare = it)
            }
        }

        viewModelScope.launch {
            preferencesRepository.themeModeFlow.collect {
                _uiState.value = _uiState.value.copy(themeMode = it)
            }
        }
    }

    fun setIncludeNewAlbums(include: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setIncludeNewAlbums(include)
        }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            preferencesRepository.setSortOrder(order)
        }
    }

    fun setCacheLimitMb(mb: Int) {
        viewModelScope.launch {
            preferencesRepository.setDiskCacheLimitMb(mb)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            mediaRepository.clearDiskCache()
            val updatedBytes = mediaRepository.getDiskCacheSizeBytes()
            _uiState.value = _uiState.value.copy(
                currentCacheSizeBytes = updatedBytes,
                currentCacheFormatted = MediaGrouping.formatStorageSize(updatedBytes)
            )
        }
    }

    fun toggleAutoplayMuted(muted: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoplayMuted(muted)
        }
    }

    fun toggleEraseExif(erase: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEraseExifOnShare(erase)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    companion object {
        fun provideFactory(
            preferencesRepository: PreferencesRepository,
            mediaRepository: MediaRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(preferencesRepository, mediaRepository) as T
            }
        }
    }
}
