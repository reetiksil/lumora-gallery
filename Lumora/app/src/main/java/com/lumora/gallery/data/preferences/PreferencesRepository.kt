package com.lumora.gallery.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lumora.gallery.data.model.SortOrder
import com.lumora.gallery.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumora_settings")

/**
 * Lightweight DataStore repository managing user settings and selected album bucket IDs.
 */
class PreferencesRepository(private val context: Context) {

    companion object {
        val KEY_SELECTED_BUCKETS = stringSetPreferencesKey("selected_bucket_ids")
        val KEY_INCLUDE_NEW_ALBUMS = booleanPreferencesKey("include_new_albums")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        val KEY_DISK_CACHE_LIMIT_MB = intPreferencesKey("disk_cache_limit_mb")
        val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        val KEY_PINNED_BUCKETS = stringSetPreferencesKey("pinned_bucket_ids")
        val KEY_AUTOPLAY_MUTED = booleanPreferencesKey("autoplay_muted")
        val KEY_SHOW_PRIVATE_ALBUM = booleanPreferencesKey("show_private_album")
        val KEY_ERASE_EXIF_ON_SHARE = booleanPreferencesKey("erase_exif_on_share")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val selectedBucketIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_BUCKETS] ?: emptySet()
    }

    val includeNewAlbumsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_INCLUDE_NEW_ALBUMS] ?: false
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val gridColumnsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_GRID_COLUMNS] ?: 3
    }

    val diskCacheLimitMbFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DISK_CACHE_LIMIT_MB] ?: 100
    }

    val sortOrderFlow: Flow<SortOrder> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SORT_ORDER] ?: SortOrder.DATE_TAKEN_DESC.name
        try {
            SortOrder.valueOf(raw)
        } catch (e: Exception) {
            SortOrder.DATE_TAKEN_DESC
        }
    }

    val pinnedBucketIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_PINNED_BUCKETS] ?: emptySet()
    }

    val autoplayVideosMutedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTOPLAY_MUTED] ?: true
    }

    val showPrivateAlbumFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_PRIVATE_ALBUM] ?: true
    }

    val eraseExifOnShareFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ERASE_EXIF_ON_SHARE] ?: true
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: ThemeMode.AMOLED_DARK.name
        try {
            ThemeMode.valueOf(raw)
        } catch (e: Exception) {
            ThemeMode.AMOLED_DARK
        }
    }

    suspend fun setSelectedBucketIds(bucketIds: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_BUCKETS] = bucketIds
        }
    }

    suspend fun toggleBucketInPhotos(bucketId: String, showInPhotos: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SELECTED_BUCKETS] ?: emptySet()
            prefs[KEY_SELECTED_BUCKETS] = if (showInPhotos) {
                current + bucketId
            } else {
                current - bucketId
            }
        }
    }

    suspend fun setIncludeNewAlbums(include: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INCLUDE_NEW_ALBUMS] = include
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GRID_COLUMNS] = columns.coerceIn(2, 5)
        }
    }

    suspend fun setDiskCacheLimitMb(limitMb: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DISK_CACHE_LIMIT_MB] = limitMb
        }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SORT_ORDER] = order.name
        }
    }

    suspend fun togglePinnedBucket(bucketId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_PINNED_BUCKETS] ?: emptySet()
            prefs[KEY_PINNED_BUCKETS] = if (current.contains(bucketId)) {
                current - bucketId
            } else {
                current + bucketId
            }
        }
    }

    suspend fun setAutoplayMuted(muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTOPLAY_MUTED] = muted
        }
    }

    suspend fun setShowPrivateAlbum(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_PRIVATE_ALBUM] = show
        }
    }

    suspend fun setEraseExifOnShare(erase: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ERASE_EXIF_ON_SHARE] = erase
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }
}
