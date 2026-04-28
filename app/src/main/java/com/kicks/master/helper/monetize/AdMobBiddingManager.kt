package com.kicks.master.helper.monetize

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobBiddingManager {

    private const val TAG = "AdMobBiddingManager"
    private var rewardedAd: RewardedAd? = null
    private var isAdLoaded = false
    private var isLoading = false

    /**
     * Initialize Google Mobile Ads SDK (for AdMob + Meta bidding)
     */
    fun initialize(context: Context) {
        //MobileAds.initialize(context) {
        //            Log.d(TAG, "AdMob SDK initialized for bidding")
        //        }
    }

    /**
     * Load Rewarded Ad from AdMob with Meta bidding mediation
     */
    fun loadRewardedAd(context: Context) {
        if (isLoading) return

        val adUnitId = "ca-app-pub-2479131401731792/7939566089"
        if (adUnitId.isNullOrEmpty()) {
            Log.d(TAG, "Ad unit ID not found")
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "AdMob rewarded ad loaded")
                rewardedAd = ad
                isAdLoaded = true
                isLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.d(TAG, "AdMob ad load failed: ${error.message}")
                rewardedAd = null
                isAdLoaded = false
                isLoading = false
            }
        })
    }

    /**
     * Show the rewarded ad like adapter-style
     */
    fun showAd(
        activity: Activity,
        onReward: () -> Unit = {},
        onAdComplete: () -> Unit = {},
        onAdFailed: () -> Unit = {},
        onAdClicked: () -> Unit = {}
    ) {
        if (!isAdLoaded || rewardedAd == null) {
            Log.w(TAG, "Ad not ready yet")
            onAdFailed()
            loadRewardedAd(activity)
            return
        }

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.d(TAG, "User clicked AdMob ad")
                onAdClicked()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "AdMob ad dismissed")
                rewardedAd = null
                isAdLoaded = false
                onAdComplete()
                loadRewardedAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "Failed to show ad: ${adError.message}")
                rewardedAd = null
                isAdLoaded = false
                onAdFailed()
            }

            override fun onAdImpression() {
                Log.d(TAG, "AdMob impression tracked")
                //dismissDialog()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "AdMob ad showing")
            }
        }

        rewardedAd?.show(activity) { rewardItem: RewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onReward()
        }
    }

    /**
     * Optional utility
     */
    fun isAdReady(): Boolean = isAdLoaded && rewardedAd != null

    fun destroy() {
        rewardedAd = null
        isAdLoaded = false
        isLoading = false
    }
}
