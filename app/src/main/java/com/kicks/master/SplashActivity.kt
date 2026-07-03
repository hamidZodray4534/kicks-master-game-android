package com.kicks.master

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.kicks.master.databinding.ActivitySplashBinding
import com.kicks.master.helper.AppManager
import com.kicks.master.helper.apicall.RetrofitClient
import com.kicks.master.helper.model.AdSetting
import com.kicks.master.helper.model.AdxAccount
import com.kicks.master.main.MainViewModel
import com.kicks.master.utills.AppDialog
import com.kicks.master.utills.PubGloryTracker
import com.kicks.master.utills.ReferrerManager
import com.kicks.master.utills.StringUtil.getVersionCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlin.compareTo

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var appManager: AppManager

    private val handler = Handler(Looper.getMainLooper())
    private var loadingProgress = 0

    // Cosmetic progress bar runnable — keeps running while API loads
    private val loadingRunnable = object : Runnable {
        override fun run() {
            if (loadingProgress < 90) {
                loadingProgress += (2..4).random()
                if (loadingProgress > 90) loadingProgress = 90
                binding.progressBarLoading.progress = loadingProgress
                binding.tvLoadingPercent.text = "$loadingProgress%"
                handler.postDelayed(this, 60L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        handleDeepLink(intent)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply window insets to prevent UI from overlapping the status bar and nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        appManager = AppManager.getInstance(this)

        // Start cosmetic progress animation
        handler.postDelayed(loadingRunnable, 300L)
        binding.tvVersion.text="Version ${BuildConfig.VERSION_NAME}"
        startAppFlow()
    }

    private fun handleDeepLink(intent: android.content.Intent?) {
        val uri = intent?.data ?: return

        // Read incoming values — all known param name variants
        val newClickId = uri.getQueryParameter("click_id")
            ?: uri.getQueryParameter("clickId")

        val newSubId = uri.getQueryParameter("X-Sub-Id")
            ?: uri.getQueryParameter("sub_id")
            ?: uri.getQueryParameter("subId")

        val newOfferData = uri.getQueryParameter("offer_data")
            ?: uri.getQueryParameter("offerData")

        Log.d(TAG, "Deep Link Captured -> clickId: $newClickId, subId: $newSubId, offerData: $newOfferData")

        // ── Case 1: No click_id in link ──────────────────────────────────────
        // Could be a plain app-open or a link with only offer_data.
        // Silently update offer_data if present — do NOT touch session.
        if (newClickId.isNullOrBlank()) {
            if (!newOfferData.isNullOrBlank()) {
                Constant.setString(this, Constant.OFFER_DATA, newOfferData)
                Log.d(TAG, "Deep Link: no click_id — silently updated offer_data only")
            }
            return
        }

        // ── Case 2: click_id present — compare with what is already saved ────
        val savedClickId = Constant.getString(this, Constant.CLICK_ID)

        if (newClickId == savedClickId) {
            // SAME referral link the user already came from.
            // Do NOT reset TRACKING_DONE — do NOT touch session data.
            Log.d(TAG, "Deep Link: click_id unchanged ($newClickId) — skip, no session change")
            return
        }

        // ── Case 3: Genuinely NEW referral click_id ──────────────────────────
        // Update all attribution data and mark tracking as pending.
        Log.d(TAG, "Deep Link: NEW click_id detected (saved='$savedClickId' → new='$newClickId') — updating attribution")

        Constant.setString(this, Constant.CLICK_ID, newClickId)
        Constant.setBoolean(this, Constant.TRACKING_DONE, false)   // mark as un-tracked

        if (!newSubId.isNullOrBlank()) {
            Constant.setString(this, Constant.SUB_ID, newSubId)
        }
        if (!newOfferData.isNullOrBlank()) {
            Constant.setString(this, Constant.OFFER_DATA, newOfferData)
        }

        // Fire PubGlory install event for this brand-new referral
        if (!newSubId.isNullOrBlank()) {
            Log.d(TAG, "Deep Link: firing PubGlory for new click_id=$newClickId")
            lifecycleScope.launch(Dispatchers.IO) {
                PubGloryTracker(this@SplashActivity).trackInstall(
                    newClickId,
                    newSubId,
                    newOfferData ?: ""
                )
            }
        }
    }

    private fun startAppFlow() {

        lifecycleScope.launch {
            if (!appManager.getIsLogin()) {
                clearAllLocalData()
                navigateToLogin()
                return@launch
            }
            var hasUpdate = false

            try {
                val splashResponse = RetrofitClient.apiService.getSplashData()

                if (splashResponse.isSuccessful && splashResponse.body()?.success == true) {
                    val splashData = splashResponse.body()!!.data

                    // ── OneSignal App ID (from dedicated API field) ─────────
                    splashData.onesignal_app_id
                        ?.takeIf { it.isNotBlank() }
                        ?.let { Constant.setString(this@SplashActivity, Constant.ONE_SIGNAL_ID, it) }

                    splashData.url?.privacy_policy?.let {
                        Constant.setString(this@SplashActivity, Constant.PRIVACY_POLICY, it)
                    }
                    splashData.url?.more_games_link?.let {
                        Constant.setString(this@SplashActivity, Constant.PLAY_STORE_GAMES_LINK, it)
                    }
                    splashData.app_update?.let { update ->
                        val serverVersion = update.version?.toIntOrNull() ?: 0
                        val currentVersion = getVersionCode(this@SplashActivity)

                        if (currentVersion < serverVersion) {
                            hasUpdate = true
                            handler.post {
                                AppDialog.update_dialog(
                                    this@SplashActivity,
                                    "on",
                                    update.title,
                                    update.subtitle
                                )
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Splash API error: ${e.message}")
            }

            if (hasUpdate) return@launch

            fetchAllDataThenNavigate()
        }
    }

    private fun clearAllLocalData() {
        try {
            // Backup essential tracking and splash data to preserve business logic
            val clickId = Constant.getString(this, Constant.CLICK_ID)
            val subId = Constant.getString(this, Constant.SUB_ID)
            val offerData = Constant.getString(this, Constant.OFFER_DATA)
            val privacyPolicy = Constant.getString(this, Constant.PRIVACY_POLICY)
            val playStoreGamesLink = Constant.getString(this, Constant.PLAY_STORE_GAMES_LINK)

            val devicePrefs = getSharedPreferences("device_prefs", android.content.Context.MODE_PRIVATE)
            val fallbackDeviceId = devicePrefs.getString("fallback_device_id", null)

            // 1. Clear AppManager data
            appManager.clearUserData()

            // 2. Clear Constant PrefManager
            Constant.clearAll(this)

            // 3. Clear all other shared preferences dynamically
            //    Skip AppManager-controlled files to avoid corrupting session state
            val skipPrefs = setOf("userpref", "userpref_plain", "km_session_backup", "device_prefs")
            val sharedPrefsDir = java.io.File(applicationInfo.dataDir, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                val listFiles = sharedPrefsDir.list()
                if (listFiles != null) {
                    for (prefFile in listFiles) {
                        val prefName = prefFile.replace(".xml", "")
                        if (prefName in skipPrefs) continue  // protect session & device prefs
                        getSharedPreferences(prefName, android.content.Context.MODE_PRIVATE).edit().clear().apply()
                    }
                }
            }

            // Restore essential data
            if (clickId.isNotEmpty()) Constant.setString(this, Constant.CLICK_ID, clickId)
            if (subId.isNotEmpty()) Constant.setString(this, Constant.SUB_ID, subId)
            if (offerData.isNotEmpty()) Constant.setString(this, Constant.OFFER_DATA, offerData)
            if (privacyPolicy.isNotEmpty()) Constant.setString(this, Constant.PRIVACY_POLICY, privacyPolicy)
            if (playStoreGamesLink.isNotEmpty()) Constant.setString(this, Constant.PLAY_STORE_GAMES_LINK, playStoreGamesLink)
            if (fallbackDeviceId != null) {
                getSharedPreferences("device_prefs", android.content.Context.MODE_PRIVATE).edit()
                    .putString("fallback_device_id", fallbackDeviceId).apply()
            }

            Log.d(TAG, "All local data cleared successfully upon entering LoginActivity.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local data: ${e.message}")
        }
    }

    private fun fetchAllDataThenNavigate() {
        lifecycleScope.launch {
            // Fetch Home Data
            Log.d(TAG, "SplashActivity: fetching home data...")
            try {
                val offerData = Constant.getString(this@SplashActivity, Constant.OFFER_DATA)
                // Decode Base64 → parse JSON → extract coins value
                val decodedCoins = if (offerData.isNotBlank()) decodeOfferData(offerData) else "Server not send code"
                val homeResponse = RetrofitClient.apiService.getHomeData(decodedCoins)
                if (homeResponse.isSuccessful && homeResponse.body()?.success == true) {
                    val homeData = homeResponse.body()!!.data
                    val user = homeData.userDetails

                    Log.d(TAG, "Home API OK → gems=${user.gems}, coins=${user.coins}")

                    // Save user balance
                    val prefs = getSharedPreferences(MainViewModel.PREFS_NAME, MODE_PRIVATE)
                    prefs.edit()
                        .putInt(MainViewModel.KEY_USER_GEMS, user.gems)
                        .putInt(MainViewModel.KEY_USER_COINS, user.coins)
                        .apply()

                    // Save Ad Settings
                    val adSettings = homeData.adSettings
                    appManager.saveAdx(
                        AdxAccount(
                            app_id    = adSettings.adx?.app_id ?: "",
                            reward_id = adSettings.adx?.reward_unit_id ?: "",
                            banner_id = adSettings.adx?.banner_unit_id ?: "",
                            inter_id  = adSettings.adx?.interstitial_unit_id ?: "",
                            id        = adSettings.adx?.id ?: 0
                        )
                    )

                    appManager.saveDigitalTurbineAdSetting(
                        AdSetting(
                            appId     = adSettings.digitalTurbine?.app_id ?: "",
                            placement = adSettings.digitalTurbine?.reward_unit_id ?: "",
                            enabled   = !(adSettings.digitalTurbine?.is_limit_reached ?: false),
                            id        = adSettings.digitalTurbine?.id ?: 0
                        )
                    )

                    appManager.saveVungleAdSetting(
                        AdSetting(
                            appId     = adSettings.vungle?.app_id ?: "",
                            placement = adSettings.vungle?.reward_unit_id ?: "",
                            enabled   = true
                        )
                    )

                    // Save CloudX ad settings
                    appManager.saveCloudXAdSetting(
                        AdSetting(
                            appId     = adSettings.cloudX?.app_id ?: "",
                            placement = adSettings.cloudX?.reward_unit_id ?: "",
                            enabled   = true,
                            id        = adSettings.cloudX?.id ?: 0
                        )
                    )

                    // Initialize CloudX SDK now that app_id is available
                    com.kicks.master.helper.monetize.CloudX_Ad.initialize(this@SplashActivity)

                    appManager.saveAdNetworkConfig(homeData.adNetworkConfiguration)
                    appManager.saveMegaOfferSettings(homeData.megaOfferSettings)

                    prefs.edit().putLong(KEY_HOME_LAST_FETCH, System.currentTimeMillis()).apply()
                    Log.d(TAG, "All data saved. Navigating to MainActivity.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Home API exception: ${e.message}")
            }

            // Navigate to Main (since we are in fetchAllDataThenNavigate, user is already logged in)
            finishWithProgress { goTo(MainActivity::class.java) }
        }
    }

    private fun decodeOfferData(encoded: String): String {
        return try {
            val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
            Log.d("MegaOfferViewModel", "► Decoded offer_data: $decoded")
            decoded
        } catch (e: Exception) {
            Log.e("MegaOfferViewModel", "► Failed to Base64-decode offer_data: ${e.message}")
            "Failed to Base64-decode offer_data: ${e.message}"
        }
    }

    private fun finishWithProgress(action: () -> Unit) {
        handler.removeCallbacks(loadingRunnable)
        // Animate to 100% quickly
        val finishRunnable = object : Runnable {
            override fun run() {
                if (loadingProgress < 100) {
                    loadingProgress += 5
                    if (loadingProgress > 100) loadingProgress = 100
                    binding.progressBarLoading.progress = loadingProgress
                    binding.tvLoadingPercent.text = "$loadingProgress%"
                    handler.postDelayed(this, 30L)
                } else {
                    binding.progressBarLoading.progress = 100
                    binding.tvLoadingPercent.text = "100%"
                    handler.postDelayed(action, 400L)
                }
            }
        }
        handler.post(finishRunnable)
    }

    private fun goTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun applyImmersiveMode() {
        // Draw edge-to-edge but set the status bar color so it doesn't look like an overlap.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.status_bar_color)
        window.navigationBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.bottom_bar_color)

        WindowInsetsControllerCompat(window, window.decorView).let { ctrl ->
            ctrl.isAppearanceLightStatusBars = false
            ctrl.isAppearanceLightNavigationBars = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "SplashActivity"
        const val KEY_HOME_LAST_FETCH = "home_data_fetch_time"
    }

    private fun navigateToLogin() {
        val existingClickId = Constant.getString(this, Constant.CLICK_ID)
        val existingSubId = Constant.getString(this, Constant.SUB_ID)
        val existingOfferData = Constant.getString(this, Constant.OFFER_DATA)
        val isTrackingDone = Constant.getBoolean(this, Constant.TRACKING_DONE)

        // If deep link provided valid tracking data that hasn't been tracked yet
        if (existingClickId.isNotBlank() && existingSubId.isNotBlank() && !isTrackingDone) {
            Log.d("Attribution", "Using Deep Link clickId: $existingClickId  subId: $existingSubId  offerData: $existingOfferData")
            lifecycleScope.launch(Dispatchers.IO) {
                PubGloryTracker(this@SplashActivity).trackInstall(
                    existingClickId,
                    existingSubId,
                    existingOfferData
                )
            }
            finishWithProgress { goTo(LoginActivity::class.java) }
            return
        }

        // Fallback to Play Store Referrer
        ReferrerManager(this).fetchReferralCode { result ->
            Log.d("Attribution", "Play Store clickId: ${result.clickId}  subId: ${result.subId}  offerData: ${result.offerData}")

            // Only track when real PubGlory referrer data exists
            if (!result.clickId.isNullOrBlank() && !result.subId.isNullOrBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    // Save offer_data only if it's a real Base64 value (not blank)
                    PubGloryTracker(this@SplashActivity).trackInstall(
                        result.clickId,
                        result.subId,
                        result.offerData ?: ""   // empty string is safe — no fake fallback
                    )
                }
            } else {
                // No real referrer — just save offer_data if present, without fake click/sub IDs
                result.offerData?.takeIf { it.isNotBlank() }?.let { offerData ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        Constant.setString(this@SplashActivity, Constant.OFFER_DATA, offerData)
                        Log.d("Attribution", "Saved offerData without PubGlory tracking: $offerData")
                    }
                }
            }
            finishWithProgress { goTo(LoginActivity::class.java) }
        }
    }

}
