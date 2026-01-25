// app/src/main/java/com/securevault/utils/AdManager.kt
package com.securevault.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.securevault.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AdManager - Manages Google AdMob SDK initialization and native ad loading
 *
 * Features:
 * - Lazy initialization of AdMob SDK
 * - Pre-loads 3 native ads for smooth scrolling performance
 * - Uses test ad units in debug builds, production units in release
 * - Exposes reactive StateFlow for UI consumption
 * - Automatic ad cleanup on disposal
 *
 * Security Note: Ads do NOT have access to password data. All password
 * information remains encrypted and isolated from the AdMob SDK.
 */
class AdManager(private val context: Context) {

    companion object {
        private const val TAG = "AdManager"
        private const val AD_CACHE_SIZE = 3

        // Test ad unit for debug builds (Google's official test ID)
        private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

        // Production ad unit for release builds
        private const val PRODUCTION_AD_UNIT_ID = "ca-app-pub-8596687606423153/5409583540"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Ad unit ID based on build type
    private val adUnitId: String
        get() = if (BuildConfig.DEBUG) {
            TEST_AD_UNIT_ID
        } else {
            PRODUCTION_AD_UNIT_ID
        }

    // Cached native ads
    private val _nativeAds = MutableStateFlow<List<NativeAd>>(emptyList())
    val nativeAds: StateFlow<List<NativeAd>> = _nativeAds.asStateFlow()

    // Initialization state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Initialize AdMob SDK
     * Call this from MainActivity.onCreate() after migration check
     */
    fun initialize() {
        if (_isInitialized.value) {
            Log.d(TAG, "AdMob already initialized")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Initializing AdMob SDK...")

                // Initialize MobileAds SDK
                MobileAds.initialize(context) { initializationStatus ->
                    Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
                    _isInitialized.value = true

                    // Pre-load ads after initialization
                    loadAds()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AdMob: ${e.message}", e)
            }
        }
    }

    /**
     * Load native ads into cache
     * Pre-loads AD_CACHE_SIZE ads for smooth list scrolling
     */
    fun loadAds() {
        if (!_isInitialized.value) {
            Log.w(TAG, "Cannot load ads - AdMob not initialized")
            return
        }

        if (_isLoading.value) {
            Log.d(TAG, "Already loading ads")
            return
        }

        _isLoading.value = true

        scope.launch {
            try {
                Log.d(TAG, "Loading native ads (ad unit: $adUnitId)...")

                val adLoader = AdLoader.Builder(context, adUnitId)
                    .forNativeAd { nativeAd ->
                        Log.d(TAG, "Native ad loaded successfully")

                        // Add to cache if not full
                        val currentAds = _nativeAds.value.toMutableList()
                        if (currentAds.size < AD_CACHE_SIZE) {
                            currentAds.add(nativeAd)
                            _nativeAds.value = currentAds

                            Log.d(TAG, "Ad cache: ${currentAds.size}/$AD_CACHE_SIZE")
                        }
                    }
                    .withAdListener(object : com.google.android.gms.ads.AdListener() {
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            Log.e(TAG, "Failed to load ad: ${error.message} (code: ${error.code})")
                            _isLoading.value = false
                        }

                        override fun onAdLoaded() {
                            Log.d(TAG, "Ad batch loaded")
                            _isLoading.value = false
                        }
                    })
                    .build()

                // Load multiple ads at once
                adLoader.loadAds(AdRequest.Builder().build(), AD_CACHE_SIZE)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading ads: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    /**
     * Get an ad from the cache
     * Automatically triggers reload when cache runs low
     */
    fun getAd(): NativeAd? {
        val currentAds = _nativeAds.value

        if (currentAds.isEmpty()) {
            Log.d(TAG, "Ad cache empty")
            loadAds() // Trigger reload
            return null
        }

        // Get first ad and remove from cache
        val ad = currentAds.first()
        _nativeAds.value = currentAds.drop(1)

        Log.d(TAG, "Dispensed ad (remaining: ${_nativeAds.value.size})")

        // Reload if cache is running low
        if (_nativeAds.value.size < AD_CACHE_SIZE / 2) {
            Log.d(TAG, "Cache running low, triggering reload")
            loadAds()
        }

        return ad
    }

    /**
     * Clean up ads to prevent memory leaks
     * Call this from ViewModel.onCleared()
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up ads")
        _nativeAds.value.forEach { ad ->
            ad.destroy()
        }
        _nativeAds.value = emptyList()
    }
}
