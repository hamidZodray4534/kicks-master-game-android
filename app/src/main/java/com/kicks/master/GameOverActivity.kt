package com.kicks.master

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kicks.master.databinding.ActivityGameOverBinding
import com.kicks.master.helper.AppManager
import com.kicks.master.helper.monetize.ads_provider
import com.kicks.master.helper.monetize.ads_provider.preloadAllRewarded

class GameOverActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GameOverActivity"

        // Intent keys for passing data in
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_BEST_SCORE = "extra_best_score"
        const val EXTRA_IS_NEW_BEST = "extra_is_new_best"
        const val EXTRA_CLAIMED = "EXTRA_CLAIMED"

        // Result codes returned to MainActivity
        const val RESULT_RETRY = Activity.RESULT_FIRST_USER          // = 1
        const val RESULT_HOME = Activity.RESULT_FIRST_USER + 1      // = 2

        // Configuration
        private const val MIN_SCORE_FOR_CLAIM = 1
        private const val POINTS_PER_GEM = 100
    }

    private var _binding: ActivityGameOverBinding? = null
    private val binding get() = _binding!!

    private var claimed = false
    private var finalScore: Int = 0
    private var bestScore: Int = 0
    private var gemReward: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            _binding = ActivityGameOverBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Apply window insets to prevent UI from overlapping the status bar and nav bar
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
                windowInsets
            }

            applyImmersiveMode()
            extractIntentData()
            setupUI()
            setupListeners()
            playEntranceAnimation()

            preloadAllRewarded(this)

            // Pre-load ad
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            finish() // Exit gracefully on catastrophic failure
        }
    }

    private fun extractIntentData() {
        finalScore = intent?.getIntExtra(EXTRA_SCORE, 0) ?: 0
        bestScore = intent?.getIntExtra(EXTRA_BEST_SCORE, 0) ?: 0
        gemReward = maxOf(1, finalScore / POINTS_PER_GEM)
    }

    private fun setupUI() {
        // Always show the score and gem reward text
        binding.tvFinalScore.text = finalScore.toString()
        binding.tvGemReward.text = "+$gemReward\nGem"

        // Always keep the main layout and retry button visible
        binding.frameLayoutClaimGem.visibility = View.VISIBLE
        binding.frameLayoutRetry.visibility = View.VISIBLE

        // Only show the "TAP TO CLAIM" button if score meets the minimum threshold
        if (finalScore >= MIN_SCORE_FOR_CLAIM) {
            binding.btnTapToClaim.visibility = View.VISIBLE
        } else {
            binding.btnTapToClaim.visibility = View.INVISIBLE
        }
    }

    private fun setupListeners() {
        // Claim layout buttons
        binding.btnTapToClaim.setOnClickListener {
            playPulseAnimation(binding.btnTapToClaim)
            handleClaimReward()
        }
        binding.frameLayoutRetry.setOnClickListener { handleRetry() }
        binding.btnGoHome.setOnClickListener { handleHome() }
        // binding.btnShare.setOnClickListener { handleShare() }

    }

    private fun playPulseAnimation(view: View) {
        try {
            val pulse = ScaleAnimation(
                1f,
                0.94f,
                1f,
                0.94f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            ).apply {
                duration = 100L
                repeatMode = Animation.REVERSE
                repeatCount = 1
            }
            view.startAnimation(pulse)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing pulse animation: ${e.message}", e)
        }
    }

    private fun handleClaimReward() {

        if (!claimed) {
            showRewardedAd()
        }
    }

    private fun showRewardedAd() {
        Log.d(TAG, "showRewardedAd – Triggering dynamic provider")

        val config = AppManager.getInstance(this).getAdNetworkConfigForSection("Game Play Screen")


        if (config != null) {
            val json = com.google.gson.Gson().toJson(config)
            ads_provider.provider(
                activity = this,
                adProviderJson = json,
                coin = gemReward.toString(),
                play_from = "mega-offer",
                task_id = "",
                onCompleteCallback = { currentGems, rewardAmount ->
                    onRewardSuccess(currentGems, rewardAmount)
                },
                onFailedCallback = {
                    Log.e(TAG, "All ad networks failed inside ads_provider")
                    Toast.makeText(this, "Ads not ready.", Toast.LENGTH_SHORT).show()
                })
        } else {
            Log.w(TAG, "No Ad config found for game_over section")
            Toast.makeText(this, "Ad not available right now.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onRewardSuccess(currentGems: Int, rewardAmount: Int) {
        if (!claimed) {
            claimed = true
            playPulseAnimation(binding.btnTapToClaim)
            val finalReward = if (rewardAmount > 0) rewardAmount else gemReward
           // Toast.makeText(this, "🎉 Reward Claimed! +$finalReward Gem", Toast.LENGTH_SHORT).show()
            handleHome(currentGems, finalReward)
        }
    }

    private fun handleRetry() {
        try {
            val resultIntent = Intent().apply { putExtra(EXTRA_CLAIMED, claimed) }
            setResult(RESULT_RETRY, resultIntent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleRetry: ${e.message}", e)
        }
    }

    private fun handleHome(currentGems: Int = 0, rewardAmount: Int = 0) {
        try {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_CLAIMED, claimed)
                putExtra("EXTRA_CURRENT_GEMS", currentGems)
                putExtra("EXTRA_REWARD_AMOUNT", rewardAmount)
            }
            setResult(RESULT_HOME, resultIntent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleHome: ${e.message}", e)
        }
    }

    private fun handleShare() {
        try {
            val shareText =
                "🚀 I scored $finalScore in Rocket Riper! Can you beat me? Best: $bestScore"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Score"))
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleShare: ${e.message}", e)
            Toast.makeText(this, "Unable to launch share dialog", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playEntranceAnimation() {
        try {
            val fadeIn = AlphaAnimation(0f, 1f).apply {
                duration = 400L
                fillAfter = true
            }
            binding.root.startAnimation(fadeIn)
        } catch (e: Exception) {
            Log.e(TAG, "Error in playEntranceAnimation: ${e.message}", e)
        }
    }

    private fun applyImmersiveMode() {
        try {
            // Draw edge-to-edge but set the status bar color so it doesn't look like an overlap.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor =
                androidx.core.content.ContextCompat.getColor(this, R.color.status_bar_color)
            window.navigationBarColor =
                androidx.core.content.ContextCompat.getColor(this, R.color.bottom_bar_color)

            WindowInsetsControllerCompat(window, window.decorView).let { ctrl ->
                ctrl.isAppearanceLightStatusBars = false
                ctrl.isAppearanceLightNavigationBars = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyImmersiveMode: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        handleHome()
    }
}
