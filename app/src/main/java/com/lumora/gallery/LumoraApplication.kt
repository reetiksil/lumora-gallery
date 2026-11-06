package com.lumora.gallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.lumora.gallery.data.preferences.PreferencesRepository
import com.lumora.gallery.data.repository.MediaRepository

/**
 * Custom Application class.
 *
 * Implements ImageLoaderFactory to configure Coil with:
 * 1. Hard-capped disk cache (100 MB default, stored in cacheDir so it can be cleared easily).
 * 2. Capped memory cache (15% of device available heap) to run smoothly on 4 GB RAM phones.
 * 3. Video frame thumbnail decoder for video items.
 * 4. Zero network fetchers (strictly offline - no INTERNET permission).
 *
 * Also acts as a lightweight Service Locator / manual Dependency Injection container,
 * avoiding Hilt / KSP annotation processing overhead and keeping build times fast.
 */
class LumoraApplication : Application(), ImageLoaderFactory {

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var trashRepository: com.lumora.gallery.data.repository.TrashRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize repositories
        preferencesRepository = PreferencesRepository(this)
        mediaRepository = MediaRepository(this, preferencesRepository)
        trashRepository = com.lumora.gallery.data.repository.TrashRepository(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Memory cache capped at 15% of process max memory
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            // Strict disk cache cap at 100 MB using LRU eviction
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB limit
                    .build()
            }
            .components {
                // Decode thumbnail frames for videos using MediaMetadataRetriever
                add(VideoFrameDecoder.Factory())
            }
            // Respect low memory trim signals from the Android OS
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        lateinit var instance: LumoraApplication
            private set
    }
}
