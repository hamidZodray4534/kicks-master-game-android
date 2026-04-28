package com.kicks.master

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.ads.AudienceNetworkAds
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.kicks.master.helper.AppManager
import com.kicks.master.helper.apicall.RetrofitClient
import com.kicks.master.helper.monetize.AdxAdapter
import com.kicks.master.helper.monetize.SdkInitManager
import com.kicks.master.utills.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference


class AppController : Application(), Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "AppController"

        lateinit var instance: AppController
            private set

        lateinit var firebaseAnalytics: FirebaseAnalytics
            private set

        lateinit var appManager: AppManager
            private set

        // Thread-safe current-activity reference (no retained Activity leaks)
        private val currentActivityRef =
            AtomicReference<WeakReference<Activity>>(WeakReference(null))




    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    @Volatile
    private var mobileAdsInitialized = false

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        instance   = this
        appManager = AppManager.getInstance(this)

        registerActivityLifecycleCallbacks(this)

        initCritical()
        initSdkStaggered()

        // Schedule OneSignal late so it doesn't block cold-start
        try {
            SdkInitManager.scheduleOneSignalLate(this)
        } catch (e: Exception) {
            Log.e(TAG, "OneSignal schedule failed: ${e.message}")
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
        appScope.cancel()
        AdxAdapter.destroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Init helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun initCritical() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        RetrofitClient.init(this)
        SessionManager.init(this)
        registerNetworkListener()
        try {
            SdkInitManager.ensureFairBid(this@AppController)
        } catch (e: Exception) {
            Log.e(TAG, "FairBid init failed: ${e.message}")
        }
    }
    private fun initSdkStaggered() {
        appScope.launch {

            // AdMob
            delay(1_000)
            if (!mobileAdsInitialized) {
                mobileAdsInitialized = true
                withContext(Dispatchers.Main) {
                    try {
                        MobileAds.initialize(this@AppController) {
                            Log.d(TAG, "MobileAds initialized")
                            // ❌ REMOVE AdxAdapter.loadAd() from here
                        }
                    } catch (e: Exception) {
                        mobileAdsInitialized = false
                        Log.e(TAG, "MobileAds init failed: ${e.message}")
                    }
                }
            }

            // Facebook Audience Network
            delay(3_000)
            withContext(Dispatchers.Main) {
                try {
                    AudienceNetworkAds.initialize(this@AppController)
                } catch (e: Exception) {
                    Log.e(TAG, "AudienceNetwork init failed: ${e.message}")
                }
            }

            // Unity
            delay(3_000)
            try {
                SdkInitManager.ensureUnity(this@AppController)
            } catch (e: Exception) {
                Log.e(TAG, "Unity init failed: ${e.message}")
            }



        }
    }

    /*private fun initSdkStaggered() {
        appScope.launch {

            // AdMob (delay so cold-start UI appears first)
            delay(1_000)
            if (!mobileAdsInitialized) {
                mobileAdsInitialized = true
                withContext(Dispatchers.Main) {
                    try {
                        MobileAds.initialize(this@AppController) {
                            Log.d(TAG, "MobileAds initialized")
                            // Pre-warm rewarded ad once SDK is ready
                            AdxAdapter.loadAd(this@AppController)
                        }
                    } catch (e: Exception) {
                        mobileAdsInitialized = false
                        Log.e(TAG, "MobileAds init failed: ${e.message}")
                    }
                }
            }

            // Facebook Audience Network
            delay(3_000)
            withContext(Dispatchers.Main) {
                try {
                    AudienceNetworkAds.initialize(this@AppController)
                } catch (e: Exception) {
                    Log.e(TAG, "AudienceNetwork init failed: ${e.message}")
                }
            }

            // Unity
            delay(3_000)
            try {
                SdkInitManager.ensureUnity(this@AppController)
            } catch (e: Exception) {
                Log.e(TAG, "Unity init failed: ${e.message}")
            }

            // FairBid
            delay(3_000)
            try {
                SdkInitManager.ensureFairBid(this@AppController)
            } catch (e: Exception) {
                Log.e(TAG, "FairBid init failed: ${e.message}")
            }
        }
    }*/

    private fun registerNetworkListener() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                mainThreadHandler.post {
                    Log.w(TAG, "Network lost")
                    // TODO: show no-internet dialog when helper is available
                    // currentActivityRef.get().get()?.let { showNoInternetDialog(it) }
                }
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ActivityLifecycleCallbacks
    // ─────────────────────────────────────────────────────────────────────────

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef.set(WeakReference(activity))
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivityRef.get().get() === activity) {
            currentActivityRef.set(WeakReference(null))
        }
    }

    fun getCurrentActivity(): Activity? = currentActivityRef.get().get()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Callback interfaces
    // ─────────────────────────────────────────────────────────────────────────

    interface HomeCallback {
        fun onYtTaskAdded(reward: String, isReward: Boolean, message: String, user_coin: String)
    }

    interface CoinCreditCallBack {
        fun onCoinCredit(reward: String, isReward: Boolean, message: String, user_coin: String)
    }
}
