package com.kicks.master.helper.monetize

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fyber.FairBid
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Rewarded
import com.fyber.fairbid.ads.rewarded.RewardedListener
import com.kicks.master.AppController
import com.kicks.master.helper.AppManager

object FairBid_Ad {

    private const val TAG = "FairBid_Ad"
    private const val MAX_RETRY = 6

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isAdShowing = false
    private var retryAttempt = 0

    private var onAdCompleteCallback: (() -> Unit)? = null
    private var onAdFailedCallback: (() -> Unit)? = null
    private var onAdClickedCallback: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var appManager: AppManager? = null


    fun initialize(context: Context, onInitialized: (() -> Unit)? = null) {
        if (isInitialized) {
            onInitialized?.invoke()
            return
        }

        if (appManager == null) {
            appManager = AppManager.getInstance(context.applicationContext)
        }

        val dtAppId = appManager?.getDigitalTurbineAdSetting()?.appId
        val activity = resolveActivity(context)
        if (dtAppId.isNullOrEmpty()) {
          //  activity?.let { AppController.syncHomeDataSilently(it) }
            // AppController.syncHomeDataSilently(context.applicationContext)
            Log.e(TAG, "initialize failed: app_id is null or empty")
            return
        }


        val started = if (activity != null) {
            FairBid.start(dtAppId, activity)
            //FairBid.showTestSuite(activity)
            true
        } else {
            val app = context.applicationContext as? Application
            if (app != null) {
                FairBid.start(dtAppId, app)
                true
            } else {
                Log.e(TAG, "initialize failed: no valid Activity or Application context")
                false
            }
        }

        if (!started) return

      /*  UserInfo.setUserId(
            AppManager.getInstance(context.applicationContext).getUser()?.u_id
        )*/

        setRewardedListener()
        requestRewarded(context.applicationContext)

        isInitialized = true
        Log.d(TAG, "FairBid initialized with appId: $dtAppId")
        onInitialized?.invoke()
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

        val dtRid = appManager?.getDigitalTurbineAdSetting()?.placement

        if (dtRid.isNullOrEmpty()) {
            Log.e(TAG, "showRewardedAd failed: reward_unit_id is null or empty")
            onAdFailed?.invoke()
            return
        }

        Log.d(TAG, "showRewardedAd: placement=$dtRid, available=${Rewarded.isAvailable(dtRid)}")

        if (Rewarded.isAvailable(dtRid)) {
            onAdCompleteCallback = onAdComplete
            onAdFailedCallback = onAdFailed
            onAdClickedCallback = onAdClicked
            isAdShowing = true
            Rewarded.show(dtRid, activity)
        } else {
            Log.w(TAG, "showRewardedAd: ad not available")
            onAdFailed?.invoke()
        }
    }


    fun requestRewarded(context: Context) {
        if (appManager == null) {
            appManager = AppManager.getInstance(context.applicationContext)
        }

        val dtRid = appManager?.getDigitalTurbineAdSetting()?.placement

        if (dtRid.isNullOrEmpty()) {
            Log.e(TAG, "requestRewarded failed: reward_unit_id is null or empty")
            return
        }

        if (!Rewarded.isAvailable(dtRid)) {
            Log.d(TAG, "requestRewarded: requesting placement=$dtRid")
            Rewarded.request(dtRid)
        } else {
            Log.d(TAG, "requestRewarded: already available, skipping")
        }
    }


    private fun setRewardedListener() {
        Rewarded.setRewardedListener(object : RewardedListener {

            override fun onShow(placement: String, impressionData: ImpressionData) {
                Log.d(TAG, "onShow: $placement")
                isAdShowing = true
                retryAttempt = 0 // reset retry on successful show
            }

            override fun onShowFailure(placement: String, impressionData: ImpressionData) {
                Log.e(TAG, "onShowFailure: $placement")
                isAdShowing = false
                invokeCallback(onAdFailedCallback)
                clearCallbacks()
            }

            override fun onRequestStart(placement: String, requestId: String) {
                Log.d(TAG, "onRequestStart: placement=$placement, requestId=$requestId")
            }

            override fun onClick(placement: String) {
                Log.d(TAG, "onClick: $placement")
                invokeCallback(onAdClickedCallback)
            }

            override fun onAvailable(placement: String) {
                Log.d(TAG, "onAvailable: $placement")
                retryAttempt = 0
            }

            override fun onUnavailable(placement: String) {
                Log.w(TAG, "onUnavailable: $placement")
            }

            override fun onCompletion(placement: String, userRewarded: Boolean) {
                Log.d(TAG, "onCompletion: placement=$placement, userRewarded=$userRewarded")
                // Invoke directly (not async) to avoid race condition with onHide clearing callbacks
                if (userRewarded) {
                    invokeCallback(onAdCompleteCallback)
                } else {
                    // User skipped or closed early without earning reward
                    invokeCallback(onAdFailedCallback)
                }
            }

            override fun onHide(placement: String) {
                Log.d(TAG, "onHide: $placement")
                isAdShowing = false
                clearCallbacks()
            }
        })
    }





    fun clearAll() {
        Log.d(TAG, "clearAll: resetting state")
        mainHandler.removeCallbacksAndMessages(null)
        isAdShowing = false
        retryAttempt = 0
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


