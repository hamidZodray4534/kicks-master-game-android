package com.kicks.master.helper.monetize

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.kicks.master.helper.NotificationRouter
import com.kicks.master.helper.OneSignalHolder
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object SdkInitManager {

    private const val TAG = "SdkInitManager"


    private val admobInitialized = AtomicBoolean(false)
    private val fbInitialized = AtomicBoolean(false)
    private val fairBidInitialized = AtomicBoolean(false)
    private val unityInitialized = AtomicBoolean(false)
    private val vungleInitialized = AtomicBoolean(false)
    private val oneSignalScheduled = AtomicBoolean(false)


    fun ensureAdMob(context: Context) {
        /*  if (!admobInitialized.compareAndSet(false, true)) return
          Log.d(TAG, "ensureAdMob: initializing AdMob on first ad request")
          MobileAds.initialize(context.applicationContext) {
              Log.d(TAG, "ensureAdMob: AdMob ready")
          }*/
    }


    fun ensureFairBid(context: Context, onReady: (() -> Unit)? = null) {
        if (!fairBidInitialized.compareAndSet(false, true)) {
            onReady?.invoke()
            return
        }
        Log.d(TAG, "ensureFairBid: initializing on first FairBid ad request")
        FairBid_Ad.initialize(context, onReady)
    }


    fun ensureUnity(context: Context, onReady: (() -> Unit)? = null) {
        if (!unityInitialized.compareAndSet(false, true)) {
            onReady?.invoke()
            return
        }
        Log.d(TAG, "ensureUnity: initializing on first Unity ad request")
        Unity_Ad.initialize(context, onReady)
    }


    fun ensureVungle(context: Context) {
        if (!vungleInitialized.compareAndSet(false, true)) return
        Log.d(TAG, "ensureVungle: initializing on first Vungle ad request")
        VungleRewardedManager.init(context)
    }

    fun resetForTest() {
        admobInitialized.set(false)
        fbInitialized.set(false)
        fairBidInitialized.set(false)
        unityInitialized.set(false)
        vungleInitialized.set(false)
        oneSignalScheduled.set(false)
    }

    fun scheduleOneSignalLate(context: Context) {
        if (!oneSignalScheduled.compareAndSet(false, true)) return
        Log.d(TAG, "scheduleOneSignalLate: OneSignal will init in 10s")

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val appId = "AppManager.getInstance(context).getSetting()?.app_config?.onesignal_app_id"

                if (appId.isNullOrBlank()) {
                    Log.d(TAG, "No OneSignal app ID configured — skipped")
                    return@postDelayed
                }

                com.onesignal.OneSignal.Debug.logLevel = com.onesignal.debug.LogLevel.NONE
                com.onesignal.OneSignal.initWithContext(context.applicationContext, appId)

                // ── Store subscription ID ────────────────────────────────────
                com.onesignal.OneSignal.User.pushSubscription.addObserver(
                    object : com.onesignal.user.subscriptions.IPushSubscriptionObserver {
                        override fun onPushSubscriptionChange(
                            state: com.onesignal.user.subscriptions.PushSubscriptionChangedState
                        ) {
                            val id = state.current.id
                            if (!id.isNullOrBlank()) {
                                OneSignalHolder.subscriptionId = id
                                Log.d(TAG, "Subscription ID stored: $id")
                            }
                        }
                    }
                )

                com.onesignal.OneSignal.User.pushSubscription.optIn()

                // ── Request permission ───────────────────────────────────────
                CoroutineScope(Dispatchers.Main).launch {
                    val accepted = com.onesignal.OneSignal.Notifications.requestPermission(true)
                    Log.d(TAG, "Notification permission accepted=$accepted")
                }

                // ── Click listener ───────────────────────────────────────────
                com.onesignal.OneSignal.Notifications.addClickListener(
                    object : INotificationClickListener {
                        override fun onClick(event: INotificationClickEvent) {
                            val appContext = context.applicationContext
                            val additionalData =
                                event.notification.additionalData   // your JSON data{}
                            val launchURL = event.notification.launchURL        // zodrewards://...

                            Log.d(
                                TAG,
                                "Notification clicked | data=$additionalData | url=$launchURL"
                            )

                            try {
                                val intent = when {

                                    additionalData != null ->
                                        NotificationRouter.getIntent(appContext, additionalData)

                                    !launchURL.isNullOrBlank() ->
                                        Intent(Intent.ACTION_VIEW, Uri.parse(launchURL)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }

                                    else ->
                                        appContext.packageManager
                                            .getLaunchIntentForPackage(appContext.packageName)
                                            ?.apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            }
                                }

                                intent?.let { appContext.startActivity(it) }

                            } catch (ex: Exception) {
                                Log.e(TAG, "Click handler error", ex)
                            }
                        }
                    }
                )

                Log.d(TAG, "OneSignal initialized successfully")

            } catch (e: Exception) {
                Log.e(TAG, "scheduleOneSignalLate error", e)
            }
        }, 10_000L)
    }


}
