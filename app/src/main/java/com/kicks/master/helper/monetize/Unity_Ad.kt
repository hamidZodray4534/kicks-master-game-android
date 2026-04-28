package com.kicks.master.helper.monetize

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.*
import com.kicks.master.helper.AppManager


object Unity_Ad {

    private const val TAG = "Unity_Ad"
    private var PLACEMENT_ID = ""
    private var isInitialized = false
    private var isAdLoaded = false

    fun initialize(context: Context, onInitialized: (() -> Unit)? = null) {
        if (isInitialized) return

        val setting = AppManager.getInstance(context).getSetting()
        val adsConfig = "setting?.ads_config"

        if (adsConfig == null) {
            Log.d(TAG, "AdsConfig is null")
            return
        }

        val gameId = "adsConfig.unity_gameid"
        val isTestMode = "adsConfig.unity_test?.toBoolean() ?: false"
        PLACEMENT_ID = "adsConfig.unity_rewardid ?: PLACEMENT_ID"

        if (gameId.isNullOrEmpty()) {
            Log.d(TAG, "Unity Game ID is null or empty")
            return
        }

        UnityAds.initialize(
            context,
            gameId,
            true,
            object : IUnityAdsInitializationListener {

                override fun onInitializationComplete() {
                    isInitialized = true
                    Log.d(TAG, "Unity Ads initialized successfully")
                    onInitialized?.invoke()
                    loadRewardedAd()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) {
                    Log.d(TAG, "Unity Ads initialization failed: $error - $message")
                }
            }
        )
    }



    fun loadRewardedAd() {
        isAdLoaded = false

        UnityAds.load(PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                if (placementId == PLACEMENT_ID) {
                    isAdLoaded = true
                    Log.d(TAG, "Unity Ad loaded: $placementId")
                }
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                isAdLoaded = false
                Log.d(TAG, "Unity Ad failed to load: $error - $message")
            }
        })
    }

    fun showRewardedAd(
        activity: Activity,
        onAdComplete: () -> Unit = {},
        onAdFailed: (() -> Unit)? = null,
        onAdClicked: (() -> Unit)? = null
    ) {
        if (!isInitialized || !isAdLoaded) {
            Log.w(TAG, "Unity Ad is not ready yet")
            onAdFailed?.invoke()
            loadRewardedAd()
            return
        }

        UnityAds.show(
            activity,
            PLACEMENT_ID,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(placementId: String?) {
                    Log.d(TAG, "Unity Ad started")
                    //dismissDialog()
                }

                override fun onUnityAdsShowClick(placementId: String?) {
                    Log.d(TAG, "Unity Ad clicked")
                    onAdClicked?.invoke()
                }

                override fun onUnityAdsShowComplete(
                    placementId: String?,
                    state: UnityAds.UnityAdsShowCompletionState?
                ) {
                    Log.d(TAG, "Unity Ad completed with state: $state")
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        onAdComplete()
                    }
                    isAdLoaded = false
                    loadRewardedAd()
                }

                override fun onUnityAdsShowFailure(
                    placementId: String?,
                    error: UnityAds.UnityAdsShowError?,
                    message: String?
                ) {
                    Log.d(TAG, "Unity Ad failed to show: $error - $message")
                    isAdLoaded = false
                    onAdFailed?.invoke()
                    loadRewardedAd()
                }
            }
        )
    }
}
