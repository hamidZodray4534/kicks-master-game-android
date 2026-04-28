package com.kicks.master.helper.monetize

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.kicks.master.helper.AppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdxAdapter {

    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false
    private var isAdLoaded = false

    private val TAG = "ads_provider"

    private var retryCount = 0
    private const val MAX_RETRIES = 2
    private var appContext: Context? = null

    fun initializeSdk(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            //    MobileAds.initialize(context) {
            //                Log.d(TAG, "MobileAds initialized")
            //            }

        }
    }

    fun loadAd(context: Context) {
        appContext = context.applicationContext
        if (isAdLoading) {
            Log.d(TAG, "Ad is already loading")
            return
        }

        val originalId = AppManager.getInstance(context).getADX()?.reward_id
        val adUnitId = originalId?.takeIf { it.isNotBlank() }

        if (adUnitId == null) {
            Log.e(TAG, "Adx Rewarded Ad Unit ID is null or empty. Skipping load.")
            return
        }

        isAdLoading = true
        val adRequest = AdManagerAdRequest.Builder().build()

        RewardedAd.load(
            context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    rewardedAd = ad
                    isAdLoading = false
                    isAdLoaded = true
                    retryCount = 0  // reset on success
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Failed to load ad: ${error.message}")
                    rewardedAd = null
                    isAdLoading = false
                    isAdLoaded = false
                    Log.e(TAG, "Ad load failed: ${error.message}.")
                    retryCount = 0  // reset for future manual attempts
                }
            })

        Log.d(TAG, "Ad is already loading" + adUnitId)
    }

    fun showAd(
        activity: Activity,
        onUserEarnedReward: (RewardItem) -> Unit,
        onAdComplete: () -> Unit = {},
        onAdFailed: () -> Unit = {},
        onAdClicked: () -> Unit = {},
    ) {
        if (rewardedAd != null && isAdLoaded) {
            var hasEarnedReward = false

            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    Log.d(TAG, "Ad clicked")
                    onAdClicked()
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed")
                    rewardedAd = null
                    isAdLoaded = false
                    loadAd(activity)

                    if (!hasEarnedReward) {
                        // Show message: user did not earn reward
                        Log.d(TAG, "User didn't finish the ad, no reward given")
                    }

                    onAdComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Failed to show ad: ${adError.message}")
                    rewardedAd = null
                    isAdLoaded = false
                    onAdFailed()
                }

                override fun onAdImpression() {
                    //dismissDialog()
                    Log.d(TAG, "Ad impression recorded")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad is showing full screen content")
                }
            }

            rewardedAd?.show(activity) { rewardItem ->
                hasEarnedReward = true
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onUserEarnedReward(rewardItem)
            }
        } else {
            Log.w(TAG, "Ad is not ready yet")
            onAdFailed()
            loadAd(activity)
        }
    }

    fun isAdReady(): Boolean {
        return rewardedAd != null && isAdLoaded
    }

    /** Destroy ALL preloaded ads (rewarded + interstitial + app-open) and reset all state. */
    fun destroy() {
        Log.d(TAG, "destroy: Clearing all preloaded AdxAdapter ads")
        // Rewarded
        rewardedAd = null
        isAdLoaded = false
        isAdLoading = false
        retryCount = 0
        appContext = null

    }


}
