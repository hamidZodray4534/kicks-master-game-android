package com.kicks.master.helper.monetize

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.facebook.ads.*
import com.kicks.master.AppController

@SuppressLint("StaticFieldLeak")
object Facebook_Ad {

    private const val TAG = "Facebook_Ad"
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var placementId: String = ""
    private var isInitialized = false
    private var isAdLoaded = false
    private var isAdCompleted = false
    private lateinit var context: Context

    private var onAdCompleteCallback: (() -> Unit)? = null
    private var onAdFailedCallback: (() -> Unit)? = null
    private var onAdClickedCallback: (() -> Unit)? = null

    fun initialize(
        context: Context,
        placementId: String,
        isTestMode: Boolean = true,
        onInitialized: (() -> Unit)? = null
    ) {
        if (isInitialized) return

        this.context = context.applicationContext
        this.placementId = placementId

        AudienceNetworkAds.initialize(context)
        if (isTestMode) {
            AdSettings.setTestMode(true)
        }
        AdSettings.setIntegrationErrorMode(AdSettings.IntegrationErrorMode.INTEGRATION_ERROR_CRASH_DEBUG_MODE)

        Log.d(TAG, "Facebook Audience Network initialized")
        isInitialized = true
        onInitialized?.invoke()
        loadAd()
    }

    fun loadAd() {
        if (!::context.isInitialized) return
        rewardedInterstitialAd = RewardedInterstitialAd(context, placementId)
        isAdLoaded = false

        rewardedInterstitialAd?.loadAd(
            rewardedInterstitialAd?.buildLoadAdConfig()
                ?.withAdListener(object : RewardedInterstitialAdListener {
                    override fun onError(ad: Ad?, adError: AdError?) {
                        Log.d(TAG, "Facebook Ad Load Error: ${adError?.errorMessage}")
                        isAdLoaded = false
                        onAdFailedCallback?.invoke()
                        clearCallbacks()
                    }

                    override fun onAdLoaded(ad: Ad?) {
                        Log.d(TAG, "Facebook Ad Loaded")
                        isAdLoaded = true
                    }

                    override fun onAdClicked(ad: Ad?) {
                        Log.d(TAG, "Facebook Ad Clicked")
                        onAdClickedCallback?.invoke()
                    }

                    override fun onLoggingImpression(ad: Ad?) {
                        Log.d(TAG, "Facebook Ad Impression Logged")
                       // dismissDialog()
                    }

                    override fun onRewardedInterstitialCompleted() {
                        Log.d(TAG, "Facebook Ad Completed")
                        isAdCompleted = true
                    }

                    override fun onRewardedInterstitialClosed() {
                        Log.d(TAG, "Facebook Ad Closed")

                        if (isAdCompleted) {
                            onAdCompleteCallback?.invoke()
                            loadAd() // Load again only if ad was completed
                        }
                       // dismissDialog()
                        
                        val activity = AppController.instance.getCurrentActivity()
                        if (activity != null) {
                            //dismiss_conDialog(activity)
                        }

                        // Do not call onAdFailed when user just closes the ad
                        isAdCompleted = false
                        clearCallbacks()
                    }
                })?.build()
        )
    }

    fun showAd(
        onAdComplete: () -> Unit = {},
        onAdFailed: (() -> Unit)? = null,
        onAdClicked: (() -> Unit)? = null
    ) {
        onAdCompleteCallback = onAdComplete
        onAdFailedCallback = onAdFailed
        onAdClickedCallback = onAdClicked

        if (rewardedInterstitialAd != null && rewardedInterstitialAd!!.isAdLoaded) {
            rewardedInterstitialAd?.show()
        } else {
            Log.w(TAG, "Facebook Ad is not loaded")
            onAdFailedCallback?.invoke()
            clearCallbacks()
            loadAd()
        }
    }

    private fun clearCallbacks() {
        onAdCompleteCallback = null
        onAdFailedCallback = null
        onAdClickedCallback = null
    }
}
