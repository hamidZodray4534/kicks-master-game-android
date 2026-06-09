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

    private fun startAppFlow() {

        lifecycleScope.launch {
            if (!appManager.getIsLogin()) {
                navigateToLogin()
                return@launch
            }
            var hasUpdate = false

            try {
                val splashResponse = RetrofitClient.apiService.getSplashData()

                if (splashResponse.isSuccessful && splashResponse.body()?.success == true) {
                    val splashData = splashResponse.body()!!.data

                    splashData.url?.privacy_policy?.let {
                        Constant.setString(this@SplashActivity, Constant.PRIVACY_POLICY, it)
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

    private fun fetchAllDataThenNavigate() {
        lifecycleScope.launch {
            // Fetch Home Data
            Log.d(TAG, "SplashActivity: fetching home data...")
            try {
                val homeResponse = RetrofitClient.apiService.getHomeData()
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


   /* private fun goToLogin(referCode: String?, playStoreCode: String?) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("refer_code", referCode)
            putExtra("play_store_refer_code", playStoreCode)
        }
        startActivity(intent)
        AnimationUtil.applyFadeTransition(this)
        finish()
    }
    interface ReferralApi {
        @POST("referral/track-install")
        suspend fun trackInstall(@Body body: InstallRequest): Response<ResponseBody>
    }*/



}
