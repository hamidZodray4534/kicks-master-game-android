package com.kicks.master.helper.monetize

import android.app.Activity
import android.content.Context
import android.util.Log
import com.vungle.ads.AdConfig
import com.vungle.ads.BaseAd
import com.vungle.ads.InitializationListener
import com.vungle.ads.RewardedAd
import com.vungle.ads.RewardedAdListener
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleError
import com.kicks.master.helper.AppManager

object VungleRewardedManager {

    private const val TAG = "VungleRewardedMgr"

    private var rewardedAd: RewardedAd? = null
    private var placementId: String = ""
    private var appId: String = ""
    private var isAdLoading = false
    private var appManager: AppManager? = null
    private var rewardCallback: (() -> Unit)? = null
    private var onAdClosedCallback: (() -> Unit)? = null

    fun init(context: Context) {

        if (VungleAds.isInitialized()) {
            Log.d(TAG, "Vungle already initialized")
            return
        }




        if (appManager == null) {
            appManager = AppManager.getInstance(context.applicationContext)
        }

        val setting = appManager?.getVungleAdSetting()

        if (setting == null || setting.appId.isBlank()) {
            Log.e(TAG, "Vungle App ID missing or invalid")
            return
        }

        appId = setting.appId
        Log.d(TAG, "Initializing Vungle with App ID: $appId")

        Log.d(TAG, "Initializing Vungle with App ID: $appId")

        VungleAds.init(context.applicationContext, appId, object : InitializationListener {
            override fun onSuccess() {
                Log.d(TAG, "Vungle SDK init onSuccess()")
            }
            override fun onError(vungleError: VungleError) {
                Log.e(TAG, "onError(): ${vungleError.localizedMessage}")
            }
        })

    }

    fun load(context: Context) {
        Log.e(TAG, "call load method")

        if (appManager == null) {
            appManager = AppManager.getInstance(context.applicationContext)
        }

        val setting = appManager?.getVungleAdSetting()

        if (setting == null || setting.placement.isBlank()) {
            Log.e(TAG, "Vungle Reward placement ID missing")
            return
        }

        this.placementId = setting.placement

        //this.placementId = appManager?.getVungleAdSetting()?.reward_unit_id.toString()

        if (!VungleAds.isInitialized()) {
            Log.e(TAG, "Vungle not initialized. Attempting to initialize...")
            init(context)
            return
        }

        if (isAdLoading) {
            Log.d(TAG, "Ad is already loading")
            return
        }

        if (rewardedAd != null && rewardedAd?.canPlayAd() == true) {
             Log.d(TAG, "Ad is already loaded and ready to play")
             return
        }

        Log.d(TAG, "Loading Vungle Ad for placement: $placementId")
        isAdLoading = true
        rewardedAd = RewardedAd(context, placementId, AdConfig()).apply {
            adListener = object : RewardedAdListener {
                override fun onAdLoaded(baseAd: BaseAd) {
                    Log.d(TAG, "Ad loaded successfully | CreativeId=${baseAd.creativeId}")
                    isAdLoading = false
                }

                override fun onAdStart(baseAd: BaseAd) {
                    Log.d(TAG, "Ad started")
                }

                override fun onAdImpression(baseAd: BaseAd) {
                    Log.d(TAG, "Ad impression recorded")
                }

                override fun onAdClicked(baseAd: BaseAd) {
                    Log.d(TAG, "Ad clicked")
                }

                override fun onAdLeftApplication(baseAd: BaseAd) {
                    Log.d(TAG, "Ad left application")
                }

                override fun onAdEnd(baseAd: BaseAd) {
                    Log.d(TAG, "Ad ended")
                    isAdLoading = false
                    onAdClosedCallback?.invoke()
                    // THERMAL FIX: Don't immediately reload after ad ends.
                    // The user just watched a video — device is already at peak temperature.
                    // Schedule refill after 5 minutes when the device has cooled.
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (VungleAds.isInitialized()) {
                            load(context)
                        }
                    }, 5 * 60_000L)
                }

                override fun onAdRewarded(baseAd: BaseAd) {
                    Log.d(TAG, "User rewarded")
                    rewardCallback?.invoke()
                    rewardCallback = null // Consume callback
                }

                override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
                    Log.e(TAG, "Ad failed to play: ${adError.message}")
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
                    Log.e(TAG, "Ad failed to load: ${adError.message}")
                    isAdLoading = false
                }
            }
            load()
        }
    }

    fun show(activity: Activity, onReward: () -> Unit, onClosed: (() -> Unit)? = null) {
        rewardCallback = onReward
        onAdClosedCallback = onClosed

        if (rewardedAd?.canPlayAd() == true) {
            rewardedAd?.play(activity)
        } else {
            Log.e(TAG, "Rewarded ad not ready/playable")
            load(activity)
        }
    }

    fun isAvailable(): Boolean {
        return rewardedAd?.canPlayAd() == true
    }

    /** Destroy the preloaded Vungle rewarded ad and reset all state. Called by InterstitialController.clearAll(). */
    fun clearAll() {
        Log.d(TAG, "clearAll: Destroying preloaded Vungle rewarded ad")
        rewardedAd = null       // Releasing reference; Vungle SDK GC handles cleanup
        isAdLoading = false
        rewardCallback = null
        onAdClosedCallback = null
        placementId = ""
    }

}





