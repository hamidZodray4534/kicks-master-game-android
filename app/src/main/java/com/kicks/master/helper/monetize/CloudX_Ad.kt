package com.kicks.master.helper.monetize

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.cloudx.sdk.CloudX
import io.cloudx.sdk.CloudXAd
import io.cloudx.sdk.CloudXError
import io.cloudx.sdk.CloudXInitializationConfiguration
import io.cloudx.sdk.CloudXInitializationListener
import io.cloudx.sdk.CloudXLogLevel
import io.cloudx.sdk.CloudXReward
import io.cloudx.sdk.CloudXRewardedAd
import io.cloudx.sdk.CloudXRewardedListener
import io.cloudx.sdk.CloudXSdkConfiguration
import com.kicks.master.AppController
import com.kicks.master.BuildConfig
import com.kicks.master.helper.AppManager

object CloudX_Ad {

    private const val TAG = "CloudX_Ad"

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isAdShowing = false

    @Volatile
    private var isLoading = false

    private var onAdCompleteCallback: (() -> Unit)? = null
    private var onAdFailedCallback: (() -> Unit)? = null
    private var onAdClickedCallback: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var appManager: AppManager? = null

    private var rewardedAd: CloudXRewardedAd? = null

    fun initialize(context: Context, onInitialized: (() -> Unit)? = null) {
        if (isInitialized) {
            onInitialized?.invoke()
            return
        }

        if (appManager == null) {
            appManager = AppManager.getInstance(context.applicationContext)
        }

        val appKey = appManager?.getCloudXAdSetting()?.appId
        if (appKey.isNullOrEmpty()) {
            Log.e(TAG, "initialize failed: app_key is null or empty")
            return
        }

        // Enable verbose logging on debug builds only
        if (BuildConfig.DEBUG) {
            CloudX.setMinLogLevel(CloudXLogLevel.VERBOSE)
            Log.d(TAG, "CloudX verbose logging enabled (debug build)")
        } else {
            CloudX.setMinLogLevel(CloudXLogLevel.NONE)
        }

        CloudX.initialize(
            context.applicationContext,
            CloudXInitializationConfiguration.builder(appKey)
                .build(),
            object : CloudXInitializationListener {
                override fun onInitialized(configuration: CloudXSdkConfiguration) {
                    isInitialized = true
                    Log.d(TAG, "CloudX SDK initialized successfully with appKey: $appKey")
                    requestRewarded(context.applicationContext)
                    onInitialized?.invoke()
                }

                override fun onInitializationFailed(cloudXError: CloudXError) {
                    Log.e(TAG, "CloudX SDK initialization failed: ${cloudXError.message}")
                    // Reset so a future call can retry
                    isInitialized = false
                }
            }
        )

    }


    fun showRewardedAd(
        activity: Activity,
        onAdComplete: () -> Unit = {},
        onAdFailed: (() -> Unit)? = null,
        onAdClicked: (() -> Unit)? = null
    ) {
        if (isAdShowing) {
            Log.w(TAG, "showRewardedAd: ad already showing, ignoring")
            return
        }

        if (appManager == null) {
            appManager = AppManager.getInstance(activity.applicationContext)
        }

        val placement = appManager?.getCloudXAdSetting()?.placement
        if (placement.isNullOrEmpty()) {
            Log.e(TAG, "showRewardedAd failed: placement is null or empty")
            onAdFailed?.invoke()
            return
        }

        val ad = rewardedAd
        if (ad == null || !ad.isAdReady) {
            Log.w(TAG, "showRewardedAd: ad not ready, placement=$placement")
            onAdFailed?.invoke()
            return
        }

        Log.d(TAG, "showRewardedAd: showing placement=$placement")

        onAdCompleteCallback = onAdComplete
        onAdFailedCallback = onAdFailed
        onAdClickedCallback = onAdClicked
        isAdShowing = true

        ad.show(activity)
    }


    fun requestRewarded(context: Context) {
        if (!isInitialized) {
            Log.w(TAG, "requestRewarded: SDK not initialized yet, skipping")
            return
        }

        if (appManager == null) {
            appManager = AppManager.getInstance(context.applicationContext)
        }

        val placement = appManager?.getCloudXAdSetting()?.placement
        if (placement.isNullOrEmpty()) {
            Log.e(TAG, "requestRewarded failed: placement is null or empty")
            return
        }

        if (rewardedAd?.isAdReady == true) {
            Log.d(TAG, "requestRewarded: already available, skipping")
            return
        }

        if (isLoading) {
            Log.d(TAG, "requestRewarded: load already in progress, skipping")
            return
        }

        Log.d(TAG, "requestRewarded: loading placement=$placement")
        isLoading = true

        val ad = CloudX.createRewarded(context.applicationContext, placement)
        ad.listener = buildRewardedListener()
        ad.load()
        rewardedAd = ad
    }


    private fun buildRewardedListener(): CloudXRewardedListener {
        return object : CloudXRewardedListener {

            override fun onAdLoaded(cloudXAd: CloudXAd) {
                Log.d(TAG, "onAdLoaded")
                isLoading = false
            }

            override fun onAdLoadFailed(adUnitId: String, error: CloudXError) {
                Log.e(TAG, "onAdLoadFailed: adUnitId=$adUnitId, error=${error.message}")
                isLoading = false
            }

            override fun onAdDisplayed(cloudXAd: CloudXAd) {
                Log.d(TAG, "onAdDisplayed")
                isAdShowing = true
            }

            override fun onAdDisplayFailed(cloudXAd: CloudXAd, error: CloudXError) {
                Log.e(TAG, "onAdDisplayFailed: ${error.message}")
                isAdShowing = false
                invokeCallback(onAdFailedCallback)
                clearCallbacks()
            }

            override fun onAdClicked(cloudXAd: CloudXAd) {
                Log.d(TAG, "onAdClicked")
                invokeCallback(onAdClickedCallback)
            }

            override fun onUserRewarded(cloudXAd: CloudXAd, reward: CloudXReward) {
                Log.d(TAG, "onUserRewarded: amount=${reward.amount}, label=${reward.label}")
                invokeCallback(onAdCompleteCallback)
            }

            override fun onAdHidden(cloudXAd: CloudXAd) {
                Log.d(TAG, "onAdHidden")
                isAdShowing = false
                clearCallbacks()
                // Preload next ad
                rewardedAd?.load()
            }
        }
    }

    fun clearAll() {
        Log.d(TAG, "clearAll: resetting state")
        mainHandler.removeCallbacksAndMessages(null)
        isAdShowing = false
        rewardedAd?.destroy()
        rewardedAd = null
        clearCallbacks()
    }

    private fun invokeCallback(callback: (() -> Unit)?) {
        callback ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                callback.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "invokeCallback: error on main thread", e)
            }
        } else {
            mainHandler.post {
                try {
                    callback.invoke()
                } catch (e: Exception) {
                    Log.e(TAG, "invokeCallback: error posting to main thread", e)
                }
            }
        }
    }

    private fun clearCallbacks() {
        onAdCompleteCallback = null
        onAdFailedCallback = null
        onAdClickedCallback = null
    }

    @Suppress("unused")
    private fun resolveActivity(context: Context): Activity? {
        if (context is Activity) return context
        return try {
            AppController.instance.getCurrentActivity()
        } catch (e: Exception) {
            Log.w(TAG, "resolveActivity: failed to get current activity", e)
            null
        }
    }
}
