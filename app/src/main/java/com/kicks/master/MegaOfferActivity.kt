package com.kicks.master

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kicks.master.databinding.ActivityMegaOfferBinding
import com.kicks.master.helper.AppManager
import com.kicks.master.helper.monetize.ads_provider
import com.kicks.master.helper.monetize.ads_provider.preloadAllRewarded
import com.kicks.master.megaoffer.MegaOfferViewModel
import com.kicks.master.utills.DialogUtils

class MegaOfferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMegaOfferBinding
    private val viewModel: MegaOfferViewModel by viewModels()

   
    private var rewardCount: Int = 0
    private var offerId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()

        binding = ActivityMegaOfferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply window insets to prevent UI from overlapping the status bar and nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        // Initialize reward_count and offerId from cached AppManager data
        setupOfferData()
        preloadAllRewarded(this)
        setupClickListeners()
        observeViewModel()

    }


    private fun setupOfferData() {
        Log.d(TAG, "━━━ setupOfferData() STARTED ━━━")
        val offer = AppManager.getInstance(this).getOffer()
        if (offer != null) {
            rewardCount = offer.rewardCount
            offerId = offer.id
            Log.d(TAG, "► Data loaded from cache: offerId=$offerId, rewardCount=$rewardCount")
            updateRewardLabel()
        } else {
            Log.w(TAG, "► No cached offer data found in AppManager")
            // Fallback to settings if offer is missing
            val settings = AppManager.getInstance(this).getMegaOfferSettings()
            rewardCount = settings?.win_coin_reward ?: 0
            offerId = settings?.id ?: 0
            updateRewardLabel()
        }
    }


    private fun updateRewardLabel() {
        val text = "Collect $rewardCount coins"
        binding.tvRateSub.text = text
        Log.d(TAG, "► tvRateSub updated to: \"$text\"")
    }

    private fun setupClickListeners() {
        binding.btnWatchAd.setOnClickListener {
            binding.btnWatchAd.isEnabled = true
            showRewardedAd()
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeViewModel() {
        viewModel.event.observe(this) { event ->
            event ?: return@observe
            DialogUtils.hideLoading()
            when (event) {
                is MegaOfferViewModel.Event.RewardEarned -> {
                    val returnIntent = Intent().apply {
                        putExtra("EXTRA_GEMS", event.gems)
                        putExtra("EXTRA_COINS", event.coins)
                        putExtra("EXTRA_REWARD_COINS", event.rewardAmount)
                    }
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }
                is MegaOfferViewModel.Event.AdFailed -> {
                    val returnIntent = Intent().apply {
                        putExtra("EXTRA_AD_FAILED", true)
                    }
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }
                is MegaOfferViewModel.Event.AdSkipped -> {
                    Toast.makeText(this, "Watch the full ad to earn your reward!", Toast.LENGTH_SHORT).show()
                    binding.btnWatchAd.isEnabled = true
                }
                is MegaOfferViewModel.Event.AlreadyClaimed -> {
                    Toast.makeText(this, "Already claim this task", Toast.LENGTH_LONG).show()
                    binding.btnWatchAd.isEnabled = true
                }
                is MegaOfferViewModel.Event.DailyLimit -> {
                    Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
                    binding.btnWatchAd.isEnabled = true
                }
                is MegaOfferViewModel.Event.Error -> {
                    showErrorDialog(event.message)
                    binding.btnWatchAd.isEnabled = true
                }
            }
            viewModel.clearEvent()
        }
    }

    private fun showRewardedAd() {
        Log.d(TAG, "━━━ showRewardedAd() — rewardCount at ad time = $rewardCount ━━━")
        Log.d(TAG, "► coin passed to ads_provider: \"${rewardCount}\"")

        val config =AppManager.getInstance(this).getAdNetworkConfigForSection("Game Play Screen")

        if (config != null) {
            val json = com.google.gson.Gson().toJson(config)
            ads_provider.provider(
                activity = this,
                adProviderJson = json,
                coin = rewardCount.toString(),
                play_from = "mega_offer",
                task_id = "",
                isClick = true,
                onCompleteCallback = { _, _ ->
                    if (Constant.getString(this@MegaOfferActivity, Constant.MEGA_OFFER_ACTIVE) != "1") {
                        Log.w(TAG, "► Ad completed but MEGA_OFFER_ACTIVE is not 1. User might not have clicked the ad.")
                        binding.btnWatchAd.isEnabled = true
                    } else {
                        binding.btnWatchAd.isEnabled = false
                        Log.d(TAG, "► Ad completed and MEGA_OFFER_ACTIVE is 1. Waiting for user to return from store.")
                        DialogUtils.showLoading(this@MegaOfferActivity)
                    }
                },
                onFailedCallback = {

                    Log.e(TAG, "All ad networks failed")
                    Toast.makeText(this, "Ad not ready.", Toast.LENGTH_SHORT).show()
                    binding.btnWatchAd.isEnabled = true
                }
            )
        } else {
            Log.w(TAG, "No Ad config found")
            Toast.makeText(this, "Ad not available right now.", Toast.LENGTH_SHORT).show()
            binding.btnWatchAd.isEnabled = true
        }
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

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        val clickId = Constant.getString(this, Constant.CLICK_ID)
        val subId = Constant.getString(this, Constant.SUB_ID)
        // Existing business logic: verify app install after returning from store
        if (Constant.getString(this, Constant.MEGA_OFFER_ACTIVE) == "1") {
          DialogUtils.showLoading(this)

            val storedMb = Constant.getString(this, Constant.RUNNING_MB).toIntOrNull() ?: 0
            val mediaSize = AppManager.getInstance(this).getMegaOfferSettings()?.media_size ?: 0
            val mbThreshold = storedMb + mediaSize
            val currentMb = com.kicks.master.utills.StringUtil.getStoreMb(this).toIntOrNull() ?: 0

            Log.d(TAG, "onResume Verify: stored=$storedMb, mediaSize=$mediaSize, current=$currentMb")

            if (currentMb >= mbThreshold) {
                Constant.setString(this, Constant.MEGA_OFFER_ACTIVE, "0")
                val settings = AppManager.getInstance(this).getMegaOfferSettings()
                val finalOfferId = if (offerId > 0) offerId else settings?.id ?: 0
               // val finalOfferId =  settings?.id ?: 0
                val offerType = settings?.slug ?: "mega-offer"

                if (finalOfferId > 0) {
                    Log.d(TAG, "► Install check passed. Calling creditMegaOffer for ID: $finalOfferId, Type: $offerType")
                    viewModel.creditMegaOffer(finalOfferId.toString(), offerType,clickId,subId)
                } else {
                    Log.e(TAG, "► Install check passed BUT offerId is unknown. Falling back to local reward logic.")
                    val gemReward = AppManager.getInstance(this).getMegaOfferSettings()?.win_gem_reward ?: 1
                    viewModel.onRewardEarned(rewardAmount = gemReward, coinReward = rewardCount)
                }
            } else {
                Constant.setString(this, Constant.MEGA_OFFER_ACTIVE, "0")
                DialogUtils.hideLoading()
                showFailedInstallDialog()
            }
        }
    }

    private fun showFailedInstallDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.failed_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        dialog.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.btnRetry).setOnClickListener {
            dialog.dismiss()
            binding.btnWatchAd.isEnabled = true
            binding.btnWatchAd.performClick()
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun showErrorDialog(message: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.failed_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        dialog.findViewById<android.widget.TextView>(R.id.tvFailedTitle)?.text = "Claim Failed"
        dialog.findViewById<android.widget.TextView>(R.id.tvFailedDesc)?.text = message
        
        dialog.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.btnRetry).setOnClickListener {
            dialog.dismiss()
            binding.btnWatchAd.isEnabled = true
            binding.btnWatchAd.performClick()
        }
        dialog.setCancelable(false)
        dialog.show()
    }
    companion object {
        private const val TAG = "MegaOfferActivity"
    }
}