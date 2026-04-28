package com.kicks.master.helper.monetize

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.kicks.master.Constant
import com.kicks.master.helper.Post_Request
import com.google.gson.Gson
import com.kicks.master.model.AdNetworkConfiguration
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ads_provider {
    private const val TAG = "ads_provider"
    private var lastShownAdNetwork: String? = null
    private var vungleImpressionCount = 0
    /* private var callback: HomeCCallback? = null
     fun setCallback(listener: HomeCCallback?) {
         callback = listener
     }

     fun clearCallback() {
         callback = null
     }*/

    private fun normalizeProvider(name: String): String {
        return name.trim().lowercase()
    }


    fun provider(
        activity: Activity,
        adProviderJson: String?,
        coin: String?,
        play_from: String?,
        task_id: String?,
        isClick: Boolean = false,
        images_id: String? = null,
        onCompleteCallback: ((currentGems: Int, rewardAmount: Int) -> Unit)? = null,
        onFailedCallback: (() -> Unit)? = null
    ) {
        if (adProviderJson.isNullOrBlank()) {
            Log.e(TAG, "adProviderJson is null or empty")
            onFailedCallback?.invoke()
            return
        }

        val adConfig = try {
            Gson().fromJson(adProviderJson, AdNetworkConfiguration::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse adProviderJson: ${e.message}")
            onFailedCallback?.invoke()
            return
        }

        if (adConfig == null) {
            Log.e(TAG, "adConfig is null after parsing")
            onFailedCallback?.invoke()
            return
        }

        if (!adConfig.ad_type.equals("Rewarded", true)) {
            Log.w(TAG, "Invalid ad_type: ${adConfig.ad_type}")
            onFailedCallback?.invoke()
            return
        }

        // Build waterfall list
        val providers = mutableListOf<String>()
        val primary = adConfig.primary_ad_network
        if (!primary.isNullOrBlank()) {
            providers.add(normalizeProvider(primary))
        }

        val fallbacks = adConfig.fallback_ad_networks
        if (!fallbacks.isNullOrBlank()) {
            providers.addAll(
                fallbacks.split(",")
                    .map { normalizeProvider(it) }
                    .filter { it.isNotBlank() }
            )
        }

        val finalProviders = providers.distinct()
        Log.d(TAG, "Ad Waterfall: $finalProviders")

        if (finalProviders.isEmpty()) {
            Log.e(TAG, "No ad providers configured")
            onFailedCallback?.invoke()
            return
        }

        val startIndex = if (Constant.getBoolean(activity, Constant.IS_PRIMARY_PAUSED)) 1 else 0

        //  Start waterfall with retry support (max 3 attempts)
        startWaterfallWithRetry(
            activity = activity,
            providers = finalProviders,
            startIndex = startIndex,
            adConfig = adConfig,
            coin = coin,
            play_from = play_from,
            task_id = task_id,
            isClick = isClick,
            images_id = images_id,
            onCompleteCallback = onCompleteCallback,
            onFailedCallback = onFailedCallback,
            currentAttempt = 1,
            maxAttempts = 3
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Retry Orchestrator
    // ─────────────────────────────────────────────────────────────

    private fun startWaterfallWithRetry(
        activity: Activity,
        providers: List<String>,
        startIndex: Int,
        adConfig: AdNetworkConfiguration,
        coin: String?,
        play_from: String?,
        task_id: String?,
        isClick: Boolean,
        images_id: String?,
        onCompleteCallback: ((currentGems: Int, rewardAmount: Int) -> Unit)?,
        onFailedCallback: (() -> Unit)?,
        currentAttempt: Int,
        maxAttempts: Int
    ) {
        Log.d(TAG, "Waterfall attempt $currentAttempt of $maxAttempts")

        showAdFromProvider(
            activity = activity,
            providers = providers,
            index = startIndex,
            adConfig = adConfig,
            coin = coin,
            play_from = play_from,
            task_id = task_id,
            isClick = isClick,
            images_id = images_id,
            onCompleteCallback = onCompleteCallback,
            onWaterfallExhausted = {
                if (activity.isFinishing) {
                    Log.w(TAG, "Activity finishing — aborting retry")
                    onFailedCallback?.invoke()
                    return@showAdFromProvider
                }

                if (currentAttempt < maxAttempts) {
                    val delayMs = when (currentAttempt) {
                        1 -> 1000L  // 1s before attempt 2
                        2 -> 2000L  // 2s before attempt 3
                        else -> 1000L
                    }
                    Log.d(TAG, "All providers failed on attempt $currentAttempt — retrying in ${delayMs}ms")

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!activity.isFinishing) {
                            startWaterfallWithRetry(
                                activity = activity,
                                providers = providers,
                                startIndex = startIndex,
                                adConfig = adConfig,
                                coin = coin,
                                play_from = play_from,
                                task_id = task_id,
                                isClick = isClick,
                                images_id = images_id,
                                onCompleteCallback = onCompleteCallback,
                                onFailedCallback = onFailedCallback,
                                currentAttempt = currentAttempt + 1,
                                maxAttempts = maxAttempts
                            )
                        } else {
                            Log.w(TAG, "Activity finished during retry delay — aborting")
                            onFailedCallback?.invoke()
                        }
                    }, delayMs)

                } else {
                    // All 3 attempts exhausted
                    Log.e(TAG, "All $maxAttempts waterfall attempts failed. Giving up.")
                    Handler(Looper.getMainLooper()).post {
                        if (!activity.isFinishing) {
                            Toast.makeText(
                                activity,
                                "Ad not ready. Please try again later.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    onFailedCallback?.invoke()
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Waterfall Runner
    // ─────────────────────────────────────────────────────────────

    private fun showAdFromProvider(
        activity: Activity,
        providers: List<String>,
        index: Int,
        adConfig: AdNetworkConfiguration,
        coin: String?,
        play_from: String?,
        task_id: String?,
        isClick: Boolean,
        images_id: String?,
        onCompleteCallback: ((currentGems: Int, rewardAmount: Int) -> Unit)?,
        onWaterfallExhausted: () -> Unit
    ) {
        if (index < 0 || index >= providers.size) {
            Log.d(TAG, "All providers exhausted at index: $index")
            onWaterfallExhausted()
            return
        }

        val adNetwork = providers[index]
        Log.d(TAG, "Trying [$index]: $adNetwork")

        val onComplete = {
            Log.d(TAG, "Ad completed from: $adNetwork")
            ad_finishPlay(
                activity, coin, play_from, task_id, isClick, images_id
            ) { currentGems, rewardAmount ->
                onCompleteCallback?.invoke(currentGems, rewardAmount)
            }
        }

        val onFailed = {
            Log.d(TAG, "[$adNetwork] failed → trying next")
            showAdFromProvider(
                activity = activity,
                providers = providers,
                index = index + 1,
                adConfig = adConfig,
                coin = coin,
                play_from = play_from,
                task_id = task_id,
                isClick = isClick,
                images_id = images_id,
                onCompleteCallback = onCompleteCallback,
                onWaterfallExhausted = onWaterfallExhausted
            )
        }

        val onClicked = {
            if (isClick && !activity.isFinishing) {
                sc_click_taskAdd(activity, coin, play_from, task_id)
            }
            Log.d(TAG, "Ad clicked: $adNetwork")
        }

        when (adNetwork) {
            "digitalturbine", "digital turbine", "digital-turbine", "fairbid" -> {
                if (Constant.getBoolean(activity, Constant.IS_PRIMARY_PAUSED)) {
                    Log.d(TAG, "DT paused — skipping to next provider")
                    onFailed()
                } else {
                    FairBid_Ad.showRewardedAd(
                        activity,
                        onAdComplete = {
                            runDTAdCallbacksWithDelay(activity)
                            onComplete()
                            preloadAllRewarded(activity)
                        },
                        onAdFailed = {
                            onFailed()
                            preloadAllRewarded(activity)
                        },
                        onAdClicked = { onClicked() }
                    )
                }
            }

            "adx" -> {
                AdxAdapter.showAd(
                    activity,
                    onUserEarnedReward = {
                        runAdCallbacksWithDelay(activity)
                        onComplete()
                        preloadAllRewarded(activity)
                    },
                    onAdComplete = {},
                    onAdFailed = {
                        onFailed()
                        preloadAllRewarded(activity)
                    },
                    onAdClicked = { onClicked() }
                )
            }

            "meta audience network", "meta" -> {
                Facebook_Ad.showAd(
                    onAdComplete = { onComplete() },
                    onAdFailed = { onFailed() },
                    onAdClicked = { onClicked() }
                )
            }

            "inmobi" -> {
                InMobi_Ad.showRewardedAd(
                    activity,
                    onAdComplete = {
                        onComplete()
                        preloadAllRewarded(activity)
                    },
                    onAdFailed = {
                        onFailed()
                        preloadAllRewarded(activity)
                    },
                    onAdClicked = { onClicked() }
                )
            }

            "unity ads" -> {
                Unity_Ad.showRewardedAd(
                    activity,
                    onAdComplete = {
                        onComplete()
                        preloadAllRewarded(activity)
                    },
                    onAdFailed = {
                        onFailed()
                        preloadAllRewarded(activity)
                    }
                )
            }

            "admob meta bidding", "admob" -> {
                AdMobBiddingManager.showAd(
                    activity,
                    onReward = { sc_click_taskAdd(activity, coin, play_from, task_id) },
                    onAdComplete = {
                        onComplete()
                        preloadAllRewarded(activity)
                    },
                    onAdFailed = {
                        onFailed()
                        preloadAllRewarded(activity)
                    },
                    onAdClicked = { onClicked() }
                )
            }

            "vungle" -> {
                if (VungleRewardedManager.isAvailable()) {
                    VungleRewardedManager.show(
                        activity,
                        onReward = {
                            onComplete()
                            preloadAllRewarded(activity)
                        },
                        onClosed = {
                            onComplete()
                            preloadAllRewarded(activity)
                        }
                    )
                } else {
                    VungleRewardedManager.load(activity)
                    onFailed()
                }
            }

            else -> {
                Log.w(TAG, "Unknown ad network: $adNetwork — skipping")
                onFailed()
            }
        }
    }


    /* fun provider(
         activity: Activity,
         adProviderJson: String?,
         coin: String?,
         play_from: String?,
         task_id: String?,
         isClick: Boolean = false,
         images_id: String? = null,
         onCompleteCallback: ((currentGems: Int, rewardAmount: Int) -> Unit)? = null,
         onFailedCallback: (() -> Unit)? = null
     ) {
         if (adProviderJson.isNullOrBlank()) {
             Log.e(TAG, "adProviderJson is null or empty")
             onFailedCallback?.invoke()
             return
         }

         val adConfig = try {
             Gson().fromJson(adProviderJson, AdNetworkConfiguration::class.java)
         } catch (e: Exception) {
             Log.e(TAG, "Failed to parse adProviderJson: ${e.message}")
             onFailedCallback?.invoke()
             return
         }

         if (adConfig == null) {
             Log.e(TAG, "adConfig is null after parsing")
             onFailedCallback?.invoke()
             return
         }

         if (!adConfig.ad_type.equals("Rewarded", true)) {
             Log.w(TAG, "Invalid ad_type: ${adConfig.ad_type}")
             return
         }

         // Build waterfall list: primary first, then fallbacks
         val providers = mutableListOf<String>()
         val primary = adConfig.primary_ad_network
         if (!primary.isNullOrBlank()) {
             providers.add(normalizeProvider(primary))
         }

         val fallbacks = adConfig.fallback_ad_networks
         if (!fallbacks.isNullOrBlank()) {
             providers.addAll(fallbacks.split(",").map { normalizeProvider(it) }
                 .filter { it.isNotBlank() })
         }

         val finalProviders = providers.distinct()

         Log.d(TAG, "Ad Waterfall: $finalProviders")

         if (finalProviders.isEmpty()) {
             Log.e(TAG, "No ad providers configured")
             onFailedCallback?.invoke()
             return
         }

         val startIndex = if (Constant.getBoolean(activity, Constant.IS_PRIMARY_PAUSED)) 1 else 0

         showAdFromProvider(
             activity,
             finalProviders,
             startIndex,
             adConfig,
             coin,
             play_from,
             task_id,
             isClick,
             images_id,
             onCompleteCallback,
             onFailedCallback
         )
     }

     private fun showAdFromProvider(
         activity: Activity,
         providers: List<String>,
         index: Int,
         adConfig: AdNetworkConfiguration,
         coin: String?,
         play_from: String?,
         task_id: String?,
         isClick: Boolean,
         images_id: String?,
         onCompleteCallback: ((currentGems: Int, rewardAmount: Int) -> Unit)? = null,
         onFailedCallback: (() -> Unit)? = null
     ) {
         // All providers exhausted
         if (index < 0 || index >= providers.size) {
             Log.d(TAG, "All providers exhausted or invalid index: $index")
             onFailedCallback?.invoke()
             return
         }

         val adNetwork = providers[index]
         Log.d(TAG, "Trying [$index]: $adNetwork")

         val onComplete = {
             Log.d(TAG, "Ad completed from: $adNetwork")
             ad_finishPlay(
                 activity, coin, play_from, task_id, isClick, images_id
             ) { currentGems, rewardAmount ->
                 onCompleteCallback?.invoke(currentGems, rewardAmount)
             }
         }

         val onFailed = {
             Log.d(TAG, "[$adNetwork] failed → trying next")
             showAdFromProvider(
                 activity,
                 providers,
                 index + 1,
                 adConfig,
                 coin,
                 play_from,
                 task_id,
                 isClick,
                 images_id,
                 onCompleteCallback,
                 onFailedCallback
             )
         }

         val onClicked = {
             if (isClick && !activity.isFinishing) {
                 sc_click_taskAdd(activity, coin, play_from, task_id)
             }
             Log.d(TAG, "Ad clicked: $adNetwork")
         }

         when (adNetwork) {
             "digitalturbine", "digital turbine", "digital-turbine", "fairbid" -> {
                 if (Constant.getBoolean(activity, Constant.IS_PRIMARY_PAUSED)) {
                     Log.d(TAG, "DT paused — skipping to next provider")
                     onFailed()
                 } else {
                     FairBid_Ad.showRewardedAd(activity, onAdComplete = {
                         runDTAdCallbacksWithDelay(activity)
                         onComplete()
                         preloadAllRewarded(activity)
                     }, onAdFailed = {
                         onFailed()
                         preloadAllRewarded(activity)
                     }, onAdClicked = { onClicked() })
                 }
             }

             "adx" -> {
                 AdxAdapter.showAd(activity,
                     onUserEarnedReward = {
                     runAdCallbacksWithDelay(activity)
                     onComplete()
                     preloadAllRewarded(activity)
                 },
                     onAdComplete = {

                     },
                     onAdFailed = {
                     onFailed()
                     preloadAllRewarded(activity)
                 },
                     onAdClicked = { onClicked() })
             }

             "meta audience network" -> {
                 Facebook_Ad.showAd(
                     onAdComplete = { onComplete() },
                     onAdFailed = { onFailed() },
                     onAdClicked = { onClicked() })
             }

             "inmobi" -> {
                 InMobi_Ad.showRewardedAd(
                     activity,
                     onAdComplete = {
                         onComplete()
                         preloadAllRewarded(activity)
                     },
                     onAdFailed = {
                         onFailed()
                         preloadAllRewarded(activity)
                     },
                     onAdClicked = { onClicked() })
             }

             "unity ads" -> {
                 Unity_Ad.showRewardedAd(
                     activity,
                     onAdComplete = {
                         onComplete()
                         preloadAllRewarded(activity)
                     },
                     onAdFailed = {
                         onFailed()
                         preloadAllRewarded(activity)
                     })
             }

             "admob meta bidding", "admob" -> {
                 AdMobBiddingManager.showAd(
                     activity,
                     onReward = { sc_click_taskAdd(activity, coin, play_from, task_id) },
                     onAdComplete = {
                         onComplete()
                         preloadAllRewarded(activity)
                     },
                     onAdFailed = {
                         onFailed()
                         preloadAllRewarded(activity)
                     },
                     onAdClicked = { onClicked() })
             }

             "vungle" -> {
                 if (VungleRewardedManager.isAvailable()) {
                     VungleRewardedManager.show(activity, onReward = {
                         onComplete()
                         preloadAllRewarded(activity)
                     }, onClosed = {
                         onComplete()
                         preloadAllRewarded(activity)
                     })
                 } else {
                     VungleRewardedManager.load(activity)
                     onFailed()
                 }
             }

             else -> {
                 Log.w(TAG, "Unknown ad network: $adNetwork")
                 onFailed()
             }
         }
     }*/









    private fun loadAd(activity: Activity, adNetwork: String) {
        when (adNetwork.lowercase()) {
            "digitalturbine", "fairbid" -> FairBid_Ad.requestRewarded(activity)
            "adx" -> AdxAdapter.loadAd(activity)
            "unity ads" -> Unity_Ad.loadRewardedAd()
            "vungle" -> VungleRewardedManager.init(activity)
        }
    }

    fun preloadAllRewarded(activity: Activity) {
        val appContext = activity.applicationContext
        Log.d(TAG, "Preloading all rewarded networks immediately")
        val handler = Handler(Looper.getMainLooper())

        // Start loading all networks with minimal stagger to avoid blocking the main thread
        // while ensuring they all start as soon as possible.
        handler.post {
            try {
                FairBid_Ad.initialize(appContext)
                FairBid_Ad.requestRewarded(appContext)
            } catch (e: Exception) { Log.e(TAG, "FairBid preload failed", e) }
        }

        handler.postDelayed({
            try { AdxAdapter.loadAd(appContext) } catch (e: Exception) { Log.e(TAG, "Adx preload failed", e) }
        }, 150)

        handler.postDelayed({
            try {
                if (!VungleRewardedManager.isAvailable()) {
                    VungleRewardedManager.init(appContext)
                    VungleRewardedManager.load(appContext)
                }
            } catch (e: Exception) { Log.e(TAG, "Vungle preload failed", e) }
        }, 300)

        handler.postDelayed({
            try { Unity_Ad.loadRewardedAd() } catch (e: Exception) { Log.e(TAG, "Unity preload failed", e) }
        }, 450)
    }

    private fun getImpressionLimit(activity: Activity, adNetwork: String): Int {
        return 10
    }

    fun ad_finishPlay(
        activity: Activity,
        coin: String? = null,
        play_from: String? = null,
        task_id: String? = null,
        isClick: Boolean = false,
        imagesId: String? = null,
        onSuccess: ((currentGems: Int, rewardAmount: Int) -> Unit)? = null
    ) {
        Log.d(TAG, "play_from: $play_from  imagesId:${imagesId} task_id:${task_id}")

        if (isClick) {
            onSuccess?.invoke(0, 0)
        } else {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val slug = play_from ?: "mega-offer"
                    val response = com.kicks.master.helper.apicall.RetrofitClient.apiService.playGameCredit(slug)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (!activity.isFinishing) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                val msg = response.body()?.message ?: "Reward credited successfully!"
                                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()

                                val data = response.body()?.data
                                val currentGems = data?.current_gems ?: 0
                                val rewardAmount = data?.reward_amount ?: 0
                                onSuccess?.invoke(currentGems, rewardAmount)
                            } else {
                                val errorMsg = response.body()?.message ?: response.message()
                                Toast.makeText(activity, "Failed: $errorMsg", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (!activity.isFinishing) {
                            Toast.makeText(activity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun sc_click_taskAdd(
        activity: Activity, coin: String? = null, play_from: String? = null, task_id: String? = null
    ) {
        if (activity.isFinishing) return
        activity.runOnUiThread {
            when (play_from) {
                "super_offer" -> {
                    // Logic for super offer
                }
                "mega_offer" -> {
                    val mb = com.kicks.master.utills.StringUtil.getStoreMb(activity)
                    Constant.setString(activity, Constant.RUNNING_MB, mb)
                    Constant.setString(activity, Constant.MEGA_OFFER_ACTIVE, "1")
                    Log.d(TAG, "Mega Offer activated: MB=$mb")
                }
            }
        }
    }

    fun runAdCallbacksWithDelay(activity: Activity) {
        val appContext = activity
        Handler(Looper.getMainLooper()).postDelayed({
            Post_Request.adx_update(appContext)
        }, 1500)
    }

    fun runDTAdCallbacksWithDelay(activity: Activity) {
        val appContext = activity
        Handler(Looper.getMainLooper()).postDelayed({
            if (!activity.isFinishing) {
                Post_Request.dt_update(appContext)
            }
        }, 1500)
    }

    fun notifyCoinData(coin: String) {
        Log.d("app run c", coin)
    }



}
