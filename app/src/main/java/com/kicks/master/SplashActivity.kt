package com.kicks.master

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.launch

/**
 * SplashActivity — the single place where all remote data is fetched on app start.
 *
 * Flow:
 *  1. Show splash + animate progress bar
 *  2. If NOT logged in → go to LoginActivity
 *  3. If logged in → call HOME API, save ALL data into AppManager + prefs, THEN go to MainActivity
 *
 * MainActivity.onCreate() only reads from already-populated prefs/AppManager — no race condition.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var appManager: AppManager

    private val handler = Handler(Looper.getMainLooper())
    private var loadingProgress = 0

    // Cosmetic progress bar runnable — keeps running while API loads
    private val loadingRunnable = object : Runnable {
        override fun run() {
            if (loadingProgress < 90) {          // cap at 90 until API responds
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

        // Decide navigation
        if (!appManager.getIsLogin()) {
            finishWithProgress { goTo(LoginActivity::class.java) }
        } else {
            fetchAllDataThenNavigate()
        }
       // finishWithProgress { goTo(LoginActivity::class.java) }
    }

    /**
     * Calls HOME API → saves ALL data (gems, coins, mega offer settings, ad configs)
     * into AppManager and MainViewModel prefs → then navigates to MainActivity.
     * On failure, navigates with whatever is already cached so the app is not stuck.
     */
    private fun fetchAllDataThenNavigate() {
        lifecycleScope.launch {
            Log.d(TAG, "SplashActivity: fetching home data...")
            try {
                val homeResponse = RetrofitClient.apiService.getHomeData()
                if (homeResponse.isSuccessful && homeResponse.body()?.success == true) {
                    val homeData = homeResponse.body()!!.data
                    val user = homeData.userDetails

                    Log.d(TAG, "Home API OK → gems=${user.gems}, coins=${user.coins}, status=${homeData.megaOfferSettings.mega_offer_status}")

                    // ── Save user balance to MainViewModel prefs (same file MainActivity reads) ──
                    val prefs = getSharedPreferences(MainViewModel.PREFS_NAME, MODE_PRIVATE)
                    prefs.edit()
                        .putInt(MainViewModel.KEY_USER_GEMS, user.gems)
                        .putInt(MainViewModel.KEY_USER_COINS, user.coins)
                        .apply()

                    // ── Save everything to AppManager ──────────────────────────────────────────
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

                    appManager.saveAdNetworkConfig(homeData.adNetworkConfiguration)
                    appManager.saveMegaOfferSettings(homeData.megaOfferSettings)

                    // Mark fresh — MainActivity won't need to re-fetch immediately
                    prefs.edit().putLong(KEY_HOME_LAST_FETCH, System.currentTimeMillis()).apply()

                    Log.d(TAG, "All data saved. Navigating to MainActivity.")
                } else {
                    Log.w(TAG, "Home API returned unsuccessful — using cached data. code=${homeResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Home API exception: ${e.message} — using cached data")
            }

            // Always navigate regardless of API success/failure
            finishWithProgress { goTo(MainActivity::class.java) }
        }
    }

    /** Completes the progress bar to 100% then runs [action]. */
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
}
