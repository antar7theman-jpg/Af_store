package com.example

import android.app.Application
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers

class AFStoreApplication : Application(), ImageLoaderFactory {
    override fun attachBaseContext(base: android.content.Context) {
        val locale = java.util.Locale("ar")
        java.util.Locale.setDefault(locale)
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val locale = java.util.Locale("ar")
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        initializeFirebase(this)
        try {
            com.example.workers.IncrementalSyncWorker.schedulePeriodic(this, intervalMinutes = 15)
        } catch (t: Throwable) {
            Log.e("AFStoreApplication", "Incremental auto-sync scheduling failed", t)
        }
    }

    companion object {
        var instance: AFStoreApplication? = null
            private set

        fun initializeFirebase(context: android.content.Context? = null) {
            val ctx = context?.applicationContext ?: instance ?: return
            if (FirebaseApp.getApps(ctx).isNotEmpty()) return

            val initialized = try {
                FirebaseApp.initializeApp(ctx)
            } catch (t: Throwable) {
                null
            }

            if (initialized == null && FirebaseApp.getApps(ctx).isEmpty()) {
                try {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:1041320238237:android:2d3363baf5394e68980874")
                        .setProjectId("gen-lang-client-0906732954")
                        .setApiKey("AIzaSyD4orBGv1rNbLt_sgxV623LhT7PfFgLOg8")
                        .setStorageBucket("gen-lang-client-0906732954.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(ctx, options)
                } catch (inner: Throwable) {
                    Log.e("AFStoreApplication", "FirebaseApp fallback initialization failed", inner)
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB disk cache
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(150)
            .allowHardware(true)
            .allowRgb565(true)
            .fetcherDispatcher(Dispatchers.IO)
            .decoderDispatcher(Dispatchers.Default)
            .respectCacheHeaders(false)
            .build()
    }
}

