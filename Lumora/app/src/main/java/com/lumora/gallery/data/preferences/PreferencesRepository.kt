package com.lumora.gallery.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    suspend fun setSelectedBucketIds(bucketIds: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_BUCKETS] = bucketIds
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
}
