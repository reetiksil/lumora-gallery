package com.lumora.gallery.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lumora.gallery.data.model.Album
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.preferences.PreferencesRepository
import com.lumora.gallery.data.repository.MediaRepository
import com.lumora.gallery.ui.navigation.TimeScale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TimelineUiState {
    object Loading : TimelineUiState
    data class Success(
        val groupedMedia: Map<String, List<MediaItem>>,
        val totalCount: Int,
        val timeScale: TimeScale = TimeScale.DAYS
    ) : TimelineUiState
    object Empty : TimelineUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val selectedBucketIds: StateFlow<Set<String>> = preferencesRepository.selectedBucketIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val onboardingCompleted: StateFlow<Boolean> = preferencesRepository.onboardingCompletedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val gridColumns: StateFlow<Int> = preferencesRepository.gridColumnsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)

    private val _timeScale = MutableStateFlow(TimeScale.DAYS)
    val timeScale: StateFlow<TimeScale> = _timeScale.asStateFlow()

    private val _isTimeScaleMode = MutableStateFlow(false)
    val isTimeScaleMode: StateFlow<Boolean> = _isTimeScaleMode.asStateFlow()

    private val _allAlbums = MutableStateFlow<List<Album>>(emptyList())
    val allAlbums: StateFlow<List<Album>> = _allAlbums.asStateFlow()

    private val _showAlbumPicker = MutableStateFlow(false)
    val showAlbumPicker: StateFlow<Boolean> = _showAlbumPicker.asStateFlow()

    // Query media flow dynamically whenever selectedBucketIds, timeScale or gridColumns change
    val uiState: StateFlow<TimelineUiState> = combine(
        selectedBucketIds.flatMapLatest { bucketIds -> mediaRepository.getMediaFlow(bucketIds) },
        _timeScale,
        gridColumns
    ) { items, currentScale, _ ->
        if (items.isEmpty()) {
            TimelineUiState.Empty
        } else {
            val grouped = when (currentScale) {
                TimeScale.YEARS -> mediaRepository.groupMediaByYear(items)
                TimeScale.MONTHS -> mediaRepository.groupMediaByMonth(items)
                TimeScale.DAYS -> mediaRepository.groupMediaByDate(items)
            }
            TimelineUiState.Success(
                groupedMedia = grouped,
                totalCount = items.size,
                timeScale = currentScale
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, TimelineUiState.Loading)

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch {
            val albums = mediaRepository.getAllAlbums()
            _allAlbums.value = albums

            // If user hasn't selected any buckets yet, automatically preselect Camera
            if (selectedBucketIds.value.isEmpty()) {
                val cameraBucket = albums.firstOrNull { it.isCamera }
                if (cameraBucket != null) {
                    preferencesRepository.setSelectedBucketIds(setOf(cameraBucket.bucketId))
                }
            }
        }
    }

    fun setTimeScale(scale: TimeScale) {
        _timeScale.value = scale
    }

    fun toggleTimeScaleMode() {
        _isTimeScaleMode.value = !_isTimeScaleMode.value
        if (!_isTimeScaleMode.value) {
            _timeScale.value = TimeScale.DAYS
        }
    }

    fun exitTimeScaleMode() {
        _isTimeScaleMode.value = false
        _timeScale.value = TimeScale.DAYS
    }

    fun openAlbumPicker() {
        _showAlbumPicker.value = true
    }

    fun closeAlbumPicker() {
        _showAlbumPicker.value = false
    }

    fun saveSelectedAlbums(bucketIds: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setSelectedBucketIds(bucketIds)
            preferencesRepository.setOnboardingCompleted(true)
            _showAlbumPicker.value = false
        }
    }

    fun setGridColumns(cols: Int) {
        viewModelScope.launch {
            preferencesRepository.setGridColumns(cols)
        }
    }

    fun cycleGridColumns() {
        val next = when (gridColumns.value) {
            2 -> 3
            3 -> 4
            4 -> 5
            else -> 2
        }
        setGridColumns(next)
    }

    companion object {
        fun provideFactory(
            mediaRepository: MediaRepository,
            preferencesRepository: PreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TimelineViewModel(mediaRepository, preferencesRepository) as T
            }
        }
    }
}
