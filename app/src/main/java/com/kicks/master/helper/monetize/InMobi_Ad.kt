package com.kicks.master.helper.monetize

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.inmobi.sdk.InMobiSdk
import com.inmobi.sdk.SdkInitializationListener
import org.json.JSONObject

object InMobi_Ad {

    private const val TAG = "InMobi_Ad"
    private var interstitialAd: InMobiInterstitial? = null
    private var isInitialized = false
    private var isAdLoaded = false

    private const val sdkPlacementId: Long = 10000078745
    private const val adPlacementId: Long = 10000478722

    @SuppressLint("Range")
    fun initialize(context: Context, onInitialized: (() -> Unit)? = null) {
        if (isInitialized) {
            onInitialized?.invoke()
            return
        }

        val consentObject = JSONObject().apply {
            put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
            put("gdpr", "0")
            put(InMobiSdk.IM_GDPR_CONSENT_IAB, "<YOUR_CONSENT_STRING>")
        }

        InMobiSdk.init(context, sdkPlacementId.toString(), consentObject, object : SdkInitializationListener {
            override fun onInitializationComplete(error: Error?) {
                if (error != null) {
                    Log.d(TAG, "InMobi SDK init failed: $error")
                    // Use context for toast or remove it for preloading safety
                } else {
                    isInitialized = true
                    Log.d(TAG, "InMobi SDK initialized")
                    onInitialized?.invoke()
                    loadRewardedAd(context)
                }
            }
        })
    }

    fun loadRewardedAd(context: Context) {
        if (!isInitialized) return

        isAdLoaded = false
        interstitialAd = InMobiInterstitial(context, adPlacementId, object : InterstitialAdEventListener() {

            override fun onAdDisplayed(ad: InMobiInterstitial, info: AdMetaInfo) {
                Log.d(TAG, "Ad displayed")
            }

            override fun onAdDismissed(ad: InMobiInterstitial) {
                Log.d(TAG, "Ad dismissed")
                isAdLoaded = false
                loadRewardedAd(context) // auto reload
            }

            override fun onRewardsUnlocked(ad: InMobiInterstitial, rewards: Map<Any, Any>) {
                Log.d(TAG, "Rewards unlocked: $rewards")
            }

            fun onAdClicked(ad: InMobiInterstitial) {
                Log.d(TAG, "Ad clicked")
            }

            fun onAdFetchSuccessful(ad: InMobiInterstitial) {
                isAdLoaded = true
                Log.d(TAG, "Ad fetched successfully")
            }

            override fun onAdFetchFailed(ad: InMobiInterstitial, status: InMobiAdRequestStatus) {
                isAdLoaded = false
                Log.d(TAG, "Ad fetch failed: $status")
            }
        })

        interstitialAd?.load()
    }

    fun showRewardedAd(
        activity: Activity,
        onAdComplete: (() -> Unit)? = null,
        onAdFailed: (() -> Unit)? = null,
        onAdClicked: (() -> Unit)? = null
    ) {
        if (interstitialAd != null && isAdLoaded && interstitialAd!!.isReady()) {
            interstitialAd!!.setListener(object : InterstitialAdEventListener() {

                override fun onAdDisplayed(ad: InMobiInterstitial, info: AdMetaInfo) {}
                override fun onAdDismissed(ad: InMobiInterstitial) {}
                fun onAdFetchSuccessful(ad: InMobiInterstitial) {}
                override fun onAdFetchFailed(ad: InMobiInterstitial, status: InMobiAdRequestStatus) {}

                fun onAdClicked(ad: InMobiInterstitial) {
                    Log.d(TAG, "User clicked the ad")
                    onAdClicked?.invoke()
                }

                override fun onRewardsUnlocked(ad: InMobiInterstitial, rewards: Map<Any, Any>) {
                    Log.d(TAG, "Reward unlocked")
                    onAdComplete?.invoke()
                }
            })

            interstitialAd!!.show()
        } else {
            Log.w(TAG, "Ad not ready")
            onAdFailed?.invoke()
            loadRewardedAd(activity)
        }
    }
}
