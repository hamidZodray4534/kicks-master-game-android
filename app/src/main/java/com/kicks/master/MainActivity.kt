package com.kicks.master

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kicks.master.databinding.ActivityMainBinding
import com.kicks.master.helper.AppManager
import com.kicks.master.main.MainViewModel
import com.kicks.master.utills.AppDialog
import com.kicks.master.utills.DialogUtils

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    // ── ViewModel ──────────────────────────────────────────────────────────────
    private val mainViewModel: MainViewModel by viewModels()

    // ── ViewBinding ────────────────────────────────────────────────────────────
    private lateinit var binding: ActivityMainBinding

    // ── Game Over launcher ─────────────────────────────────────────────────────
    private lateinit var gameOverLauncher: ActivityResultLauncher<Intent>
    private lateinit var megaOfferLauncher: ActivityResultLauncher<Intent>

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Log.d(TAG, "Notification permission granted")
        else Log.w(TAG, "Notification permission denied")
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private var bestScore: Int = 0
    private var currentScore: Int = 0
    private var gameReady = false
    private var pendingStart = false
    private var isPlaying = false
    private var hasGameLayoutStarted = false

    private lateinit var assetLoaderInstance: WebViewAssetLoader

    private val PREFS = "rocket_space_prefs"
    private val KEY_BEST = "best_score"
    private val KEY_SOUND = "sound_enabled"
    private var isSoundEnabled = true

    private val KEY_OFFER_PROGRESS = "offer_progress"
    private var currentOfferProgress = 0
    private val KEY_OFFER_CLAIM_COUNT = "offer_claim_count"
    private var megaOfferClaimCount = 0

    private var shineAnimator: android.animation.ValueAnimator? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable remote debugging
        WebView.setWebContentsDebuggingEnabled(true)

        // Prevent Android 15 forced edge-to-edge from pushing content under system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Apply immersive mode after the decor view is attached and measured
        window.decorView.post { applyImmersiveMode() }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply window insets to UI layers only, so the WebView (game) doesn't get squished
        ViewCompat.setOnApplyWindowInsetsListener(binding.swipeRefreshLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }
        /* ViewCompat.setOnApplyWindowInsetsListener(binding.gameOverScreen) { view, windowInsets ->
             val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
             view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
             windowInsets
         }*/


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets -> insets }

        bestScore = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BEST, 0)

        // Register GameOverActivity result handler BEFORE setupClickListeners
        gameOverLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val maxProgress = getMaxOfferProgress()
            val claimed = result.data?.getBooleanExtra("EXTRA_CLAIMED", false) ?: false
            val currentGems = result.data?.getIntExtra("EXTRA_CURRENT_GEMS", 0) ?: 0
            val rewardAmount = result.data?.getIntExtra("EXTRA_REWARD_AMOUNT", 0) ?: 0
            var perfectlyReached = false
            if (claimed) {
                if (currentOfferProgress < maxProgress) {
                    val addAmount = if (rewardAmount > 0) rewardAmount else 1
                    currentOfferProgress += addAmount
                    if (currentOfferProgress > maxProgress) currentOfferProgress = maxProgress
                    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putInt(KEY_OFFER_PROGRESS, currentOfferProgress).apply()
                    if (currentOfferProgress == maxProgress) {
                        perfectlyReached = true
                    }
                    updateOfferProgressUI()
                }

                if (currentGems > 0) {
                    mainViewModel.updateGems(this, currentGems)
                }
            }

            // Decide where to go next
            val displayGems = mainViewModel.userGems.value ?: 0
            when (result.resultCode) {
                GameOverActivity.RESULT_RETRY -> {
                    if (perfectlyReached || displayGems >= maxProgress) showSplashScreen() else startGame()
                }

                GameOverActivity.RESULT_HOME -> showSplashScreen()
                else -> showSplashScreen()
            }

            if (perfectlyReached) {
                showTargetReachedDialog()
            }
        }

        megaOfferLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data
                if (data?.getBooleanExtra("EXTRA_AD_FAILED", false) == true) {
                    mainViewModel.onAdFailedToShow()
                } else {
                    val gems = data?.getIntExtra("EXTRA_GEMS", 0) ?: 0
                    val coins = data?.getIntExtra("EXTRA_COINS", 0) ?: 0
                    val rewardCoins = data?.getIntExtra("EXTRA_REWARD_COINS", 0) ?: 0
                    if (rewardCoins > 0) {
                        mainViewModel.onMegaOfferClaimSuccess(this, gems, coins, rewardCoins)
                    } else if (gems > 0 || coins > 0) {
                        // Fallback in case rewardCoins is missing but gems/coins are present
                        mainViewModel.onRewardEarned(this, gems, coins, isMegaOffer = true)
                    } else {
                        mainViewModel.onAdSkipped()
                    }
                }
            } else {
                mainViewModel.onAdSkipped()
            }
        }

        setupWebView()
        setupClickListeners()

        // Initialize sound state
        isSoundEnabled =
            getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SOUND, true)
        //   updateSoundButtonUI()

        updateHighScoreDisplay()

        currentOfferProgress =
            getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_OFFER_PROGRESS, 0)
        megaOfferClaimCount =
            getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_OFFER_CLAIM_COUNT, 0)
        updateOfferProgressUI()

        //  startShineAnimation()

        // ── Gems & UI setup ───────────────────────────────────────────────────
        // Splash already fetched all data — just read from prefs here for instant display
        mainViewModel.loadGems(this)
        observeViewModel()

        // ── Swipe-to-refresh: pull down on home screen to reload data ─────────
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.rr_orange, R.color.white
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            mainViewModel.fetchHomeData(this, forceRefresh = true)
            binding.swipeRefreshLayout.isRefreshing = false
        }


        Log.d(TAG, "Rewarded ad pre-loading disabled")

        // Load using the AssetLoader virtual domain for CORS compatibility
        binding.gameWebView.loadUrl("https://appassets.androidplatform.net/assets/games/index.html")

        checkNotificationPermission()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.gameWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
        }
        binding.gameWebView.clearCache(true)
        binding.gameWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        binding.gameWebView.addJavascriptInterface(GameBridge(), "Android")
        binding.gameWebView.addJavascriptInterface(GameBridge(), "MainJavaClass")

        binding.gameWebView.webChromeClient = WebChromeClient()

        assetLoaderInstance = androidx.webkit.WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        binding.gameWebView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoaderInstance.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectGameBridge()
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                false
        }
    }

    private fun setupClickListeners() {
        // The PLAY button (now a FrameLayout in the new UI)
        binding.btnPlayGame.setOnClickListener {

            val appManager = AppManager.getInstance(this)
            val offerSettings = appManager.getMegaOfferSettings()
            val status = offerSettings?.mega_offer_status ?: "INACTIVE"


            if (status == "ACTIVE") {
                Log.d(TAG, "► Mega Offer is already ACTIVE. Opening ad directly.")
                showMegaOfferActiveDialog()
                return@setOnClickListener
            }


            val maxProgress = getMaxOfferProgress()
            val currentGems = mainViewModel.userGems.value ?: 0
            if (currentGems >= maxProgress) {
                showTargetReachedDialog()
                return@setOnClickListener
            }
            if (gameReady) {
                startGame()
            } else {
                pendingStart = true
                startGame()
            }
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnClaimReward.setOnClickListener {
            val appManager = AppManager.getInstance(this)
            val offerSettings = appManager.getMegaOfferSettings()
            val status = offerSettings?.mega_offer_status ?: "INACTIVE"

            if (status == "COMPLETED") {
                AppDialog.dailyLimitDialog(this)
                return@setOnClickListener
            }

            if (status == "ACTIVE") {
                Log.d(TAG, "► Mega Offer is already ACTIVE. Opening ad directly.")
                showRewardedAd()
                return@setOnClickListener
            }

            val maxProgress = getMaxOfferProgress()
            val currentGems = mainViewModel.userGems.value ?: 0
            if (currentGems >= maxProgress) {
                val id =
                    appManager.getOffer()?.id?.toString() ?: offerSettings?.id?.toString() ?: "1"
                val slug = offerSettings?.slug ?: "mega-offer"
                Log.d(TAG, "► Proceeding to unlock Mega Offer with ID: $id")
                mainViewModel.unlockMegaOffer(this, id, slug)
            }
        }
    }

    private fun updateOfferProgressUI() {
        val maxProgress = getMaxOfferProgress()
        val currentGems = mainViewModel.userGems.value ?: 0
        val displayProgress = if (currentGems > maxProgress) maxProgress else currentGems
        val offerSettings = AppManager.getInstance(this).getMegaOfferSettings()
        val status = offerSettings?.mega_offer_status ?: "INACTIVE"
        
        // Unified check for completion: either gems reached target OR it's already unlocked/claimed
        val isCompleted = displayProgress >= maxProgress || status == "ACTIVE" || status == "COMPLETED"

        binding.tvOfferCount.text = megaOfferClaimCount.toString()
        binding.pbCollectionProgress.max = maxProgress

        if (isCompleted) {
            // Case: Completed, Active, or Already Claimed (Show full progress)
            binding.pbCollectionProgress.progress = maxProgress
            binding.tvProgressText.text = "PROGRESS COMPLETED"
            binding.tvProgressPercent.text = "100%"
            binding.tvProgressPercent.visibility = View.VISIBLE
            binding.tvProgressFraction.visibility = View.GONE
            binding.btnClaimReward.visibility = View.VISIBLE
            binding.flProgressView.visibility = View.VISIBLE
            binding.frameParentProgressBar.setBackgroundResource(R.drawable.white_progress_bar_bg)

        } else {
            // In Progress
            binding.pbCollectionProgress.progress = displayProgress
            val percent = if (maxProgress > 0) (displayProgress * 100) / maxProgress else 0
            binding.tvProgressPercent.text = "${percent}%"
            binding.tvProgressPercent.visibility = View.VISIBLE
            val fractionStr =
                if (displayProgress < 10) "0$displayProgress/$maxProgress" else "$displayProgress/$maxProgress"
            binding.tvProgressFraction.text = fractionStr
            binding.tvProgressFraction.visibility = View.VISIBLE
            binding.btnClaimReward.visibility = View.GONE
            binding.flProgressView.visibility = View.VISIBLE
            binding.tvProgressText.text = "COLLECTION PROGRESS"
            binding.frameParentProgressBar.setBackgroundResource(R.drawable.custom_progress_bar_bg)
        }
    }

    private fun showTargetReachedDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_target_reached)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        val displayProgress = currentOfferProgress
        val tvTargetMessage = dialog.findViewById<android.widget.TextView>(R.id.tvTargetMessage)
        val text = "You've collected $displayProgress gems! You can\nnow open the Mega Offer Box."
        val spannableString = android.text.SpannableString(text)
        val startIndex = text.indexOf("$displayProgress gems!")
        if (startIndex >= 0) {
            spannableString.setSpan(
                android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#F74C60")),
                startIndex,
                startIndex + "$displayProgress gems!".length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvTargetMessage.text = spannableString

        dialog.findViewById<android.view.View>(R.id.ivCloseTargetDialog).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<android.view.View>(R.id.btnClaimLaterDialog).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<android.view.View>(R.id.btnOpenOfferDialog).setOnClickListener {
            dialog.dismiss()
            binding.btnClaimReward.performClick()
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showUnlockSuccessDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_target_reached)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Change Text for Success
        dialog.findViewById<android.widget.TextView>(R.id.tvTargetTitle).text = "Unlock Successful!"
        dialog.findViewById<android.widget.TextView>(R.id.tvTargetMessage).text =
            "Your Mega Offer Box is unlocked now. Claim rewards!"

        val btnOpen = dialog.findViewById<android.view.ViewGroup>(R.id.btnOpenOfferDialog)
        val btnClose = dialog.findViewById<android.view.ViewGroup>(R.id.btnClaimLaterDialog)

        (btnOpen?.getChildAt(0) as? android.widget.TextView)?.text = "Claim Reward"
        (btnClose?.getChildAt(0) as? android.widget.TextView)?.text = "Close"

        dialog.findViewById<android.view.View>(R.id.ivCloseTargetDialog)
            .setOnClickListener { dialog.dismiss() }
        btnClose?.setOnClickListener { dialog.dismiss() }
        btnOpen?.setOnClickListener {
            dialog.dismiss()
            showRewardedAd()
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showMegaOfferActiveDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_target_reached)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Change Text for ACTIVE state
        dialog.findViewById<android.widget.TextView>(R.id.tvTargetTitle).text = "Mega Offer Active!"
        dialog.findViewById<android.widget.TextView>(R.id.tvTargetMessage).text =
            "Your Mega Offer is already active. Please claim it first!"

        val btnOpen = dialog.findViewById<android.view.ViewGroup>(R.id.btnOpenOfferDialog)
        val btnClose = dialog.findViewById<android.view.ViewGroup>(R.id.btnClaimLaterDialog)

        (btnOpen?.getChildAt(0) as? android.widget.TextView)?.text = "Claim Now"
        (btnClose?.getChildAt(0) as? android.widget.TextView)?.text = "Close"

        dialog.findViewById<android.view.View>(R.id.ivCloseTargetDialog)
            .setOnClickListener { dialog.dismiss() }
        btnClose?.setOnClickListener { dialog.dismiss() }
        btnOpen?.setOnClickListener {
            dialog.dismiss()
            showRewardedAd()
        }
        dialog.setCancelable(false)
        dialog.show()
    }


    private fun dispatchNativeTapToWebView() {
        val webView = binding.gameWebView
        val yRatios = listOf(0.51f, 0.55f, 0.584f, 0.61f) // Added 0.51f for current game Play button (652/1280)

        yRatios.forEachIndexed { index, yRatio ->
            val triggerDelay = 100L + (index * 150L) // Sparks at 100ms, 250ms, 400ms
            webView.postDelayed({
                if (!isPlaying || hasGameLayoutStarted) return@postDelayed
                val w = webView.width
                val h = webView.height
                if (w == 0 || h == 0) return@postDelayed

                val x = w * 0.5f
                val y = h * yRatio
                val downTime = SystemClock.uptimeMillis()

                Log.d(TAG, "Native tap #$index at y=$yRatio ($y px / $h px)")

                val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
                webView.dispatchTouchEvent(down)
                down.recycle()

                webView.postDelayed({
                    val upTime = SystemClock.uptimeMillis()
                    val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
                    webView.dispatchTouchEvent(up)
                    up.recycle()
                }, 50)
            }, triggerDelay)
        }
    }

    private fun showSplashScreen() {
        com.kicks.master.utills.DialogUtils.hideLoading()

        isPlaying = false
        gameReady = false

        // ✅ Hide first, reload after — prevents HTML game menu flash
        binding.gameWebView.alpha      = 0f
        binding.gameWebView.visibility = View.GONE

        // Reload in background after a short delay
        binding.gameWebView.postDelayed({ binding.gameWebView.reload() }, 300)

        updateHighScoreDisplay()

        binding.swipeRefreshLayout.visibility = View.VISIBLE
        binding.splashScreen.visibility       = View.VISIBLE
        binding.splashScreen.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        )

        applyImmersiveMode()
        binding.btnPlayGame.isEnabled = true
    }


    private fun injectGameBridge() {
        try {
            val script = assets.open("games/inject.js").bufferedReader().use { it.readText() }
            Log.d(TAG, "INJECTING BRIDGE: inject.js (${script.length} bytes)")
            binding.gameWebView.evaluateJavascript(script, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting JavaScript: ${e.message}")
        }
    }

    private fun startGame() {
        isPlaying    = true
        currentScore = 0
        binding.tvCoins.text = "0"

        // Redundant injection just to be absolute sure bridge is live
        injectGameBridge()

        DialogUtils.showLoading(this)
        applyGameImmersiveMode()

        // ✅ VISIBLE so it receives touch events, but TRANSPARENT so user sees nothing
        binding.gameWebView.visibility = View.VISIBLE
        binding.gameWebView.alpha      = 0.0f   // revealed only in revealGame()


        binding.gameWebView.evaluateJavascript(
            "if(typeof _androidStartGame==='function') _androidStartGame();", null
        )

        hasGameLayoutStarted = false
        dispatchNativeTapToWebView()

        // Fallback: if game layout never confirmed after 2.5s, force-reveal anyway
        binding.gameWebView.postDelayed({
            if (isPlaying && !hasGameLayoutStarted) {
                Log.d(TAG, "Fallback: force-revealing game after 2.5s")
                onGameStarted()
                applySoundToGame()
            }
        }, 2500)

    }



    private fun revealGame() {
        com.kicks.master.utills.DialogUtils.hideLoading()

        binding.splashScreen.visibility       = View.GONE
        binding.swipeRefreshLayout.visibility = View.GONE

        binding.gameWebView.bringToFront()
        binding.gameWebView.visibility = View.VISIBLE
        binding.gameWebView.alpha      = 1.0f   // ← now the game is visible
    }


    private fun handleGameOver(score: Int) {
        if (!isPlaying) return

        runOnUiThread {


            // ✅ STEP 1: Hide WebView IMMEDIATELY — prevents any HTML flash
            binding.gameWebView.visibility = View.GONE
            binding.gameWebView.alpha = 0f

            // ✅ STEP 2: Stop playing state
            isPlaying = false

            // ✅ STEP 3: Hide loading dialog if still showing
            com.kicks.master.utills.DialogUtils.hideLoading()

            // ✅ STEP 4: Use tracked currentScore as fallback if JS passed 0
            val finalScore = if (score > 0) score else currentScore

            // ✅ STEP 5: Update best score
            val isNewBest = finalScore > bestScore
            if (isNewBest) {
                bestScore = finalScore
                saveHighScore(bestScore)
            }

            // ✅ STEP 6: Launch GameOverActivity — pure native, no HTML visible
            val intent = Intent(this, GameOverActivity::class.java).apply {
                putExtra(GameOverActivity.EXTRA_SCORE, finalScore)
                putExtra(GameOverActivity.EXTRA_BEST_SCORE, bestScore)
                putExtra(GameOverActivity.EXTRA_IS_NEW_BEST, isNewBest)
            }
            gameOverLauncher.launch(intent)

            // ✅ Use a smooth fade — avoids any jarring transition that might show HTML
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            DialogUtils.hideLoading()
        }
    }




    private fun onGameStarted() {
        runOnUiThread {
            hasGameLayoutStarted = true
            if (isPlaying) {
                // Delay revealing the webview by 150ms to ensure Construct 3 has
                // fully rendered the first frame of the new "Game" layout, avoiding the menu flash.
                binding.gameWebView.postDelayed({
                    revealGame()
                }, 50)
            }
        }
    }




    private fun onRuntimeReady() {
        gameReady = true
        runOnUiThread {
            if (pendingStart && !isPlaying) {
                pendingStart = false
                binding.btnPlayGame.isEnabled = true
                startGame()
            }

        }
    }

    private fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        saveSoundState(isSoundEnabled)
        //updateSoundButtonUI()

    }



    private fun saveSoundState(enabled: Boolean) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SOUND, enabled)
            .apply()
    }

    private fun applySoundToGame() {
        val enabled = false
        binding.gameWebView.evaluateJavascript(
            "if(typeof _androidSetSound==='function') _androidSetSound($enabled);", null
        )
    }

    private fun saveHighScore(score: Int) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_BEST, score).apply()
    }

    private fun updateHighScoreDisplay() {
        binding.tvHighestScore.text = bestScore.toString()
    }

    inner class GameBridge {
        @JavascriptInterface
        fun onGameOver(score: Int) {
            DialogUtils.showLoading(this@MainActivity)
            Log.d(TAG, "BRIDGE: onGameOver($score) called")
            handleGameOver(score)
        }

        @JavascriptInterface
        fun myScore(score: Int) {
            DialogUtils.showLoading(this@MainActivity)
            Log.d(TAG, "BRIDGE: myScore($score) called")
            handleGameOver(score)
        }

        @JavascriptInterface
        fun onHomeClicked() {
            Log.d(TAG, "BRIDGE: onHomeClicked called")
            runOnUiThread {
                showSplashScreen()
            }
        }

        @JavascriptInterface
        fun onRuntimeReady() {
            this@MainActivity.onRuntimeReady()
        }

        @JavascriptInterface
        fun onGameStarted() {
            this@MainActivity.onGameStarted()
        }

        @JavascriptInterface
        fun onScoreUpdate(score: Int) {
            if (score > 0) {
                currentScore = score
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun getMaxOfferProgress(): Int {
        return AppManager.getInstance(this).getMegaOfferSettings()?.required_gems ?: 5
    }

    private fun applyImmersiveMode() {
        // Draw edge-to-edge so the custom app bar UI is visible at the top,
        // but set the status bar color so it doesn't look like an overlap.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor =
            androidx.core.content.ContextCompat.getColor(this, R.color.main_background_color)
        window.navigationBarColor =
            androidx.core.content.ContextCompat.getColor(this, R.color.main_background_color)

        WindowInsetsControllerCompat(window, window.decorView).let { ctrl ->
            // Ensure icons are light since the background is dark
            ctrl.show(WindowInsetsCompat.Type.systemBars())
            ctrl.isAppearanceLightStatusBars = false
            ctrl.isAppearanceLightNavigationBars = false
        }
    }

    @Suppress("DEPRECATION")
    private fun applyGameImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)

        WindowInsetsControllerCompat(window, window.decorView).let { ctrl ->
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
            ctrl.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        binding.gameWebView.setPadding(0, 0, 0, 0)
        binding.root.setPadding(0, 0, 0, 0)
    }

    // Re-apply immersive mode whenever the window regains focus (e.g. after dialogs)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (isPlaying) applyGameImmersiveMode() else applyImmersiveMode()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.gameWebView.onResume()
        if (isPlaying) applyGameImmersiveMode() else applyImmersiveMode()
        mainViewModel.fetchHomeData(this, forceRefresh = true)
    }

    override fun onPause() {
        super.onPause()
        binding.gameWebView.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            isPlaying -> showSplashScreen()
            else -> super.onBackPressed()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Rewarded Ads & Gems (MVVM wiring)
    // ══════════════════════════════════════════════════════════════════════════

    private fun showRewardedAd() {
        Log.d(TAG, "showRewardedAd – Launching MegaOfferActivity")
        val intent = Intent(this, MegaOfferActivity::class.java)
        megaOfferLauncher.launch(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Observe MainViewModel events that drive reward/gem UI updates.
     */
    private fun observeViewModel() {
        mainViewModel.unlockEvent.observe(this) { event ->
            event ?: return@observe
            when (event) {
                is MainViewModel.UnlockEvent.Success -> {
                    showUnlockSuccessDialog()
                }

                is MainViewModel.UnlockEvent.AlreadyUnlocked -> {
                    Log.d(TAG, "► UnlockEvent.AlreadyUnlocked: Proceeding to claim directly.")
                    showRewardedAd()
                }

                is MainViewModel.UnlockEvent.NotEnoughGems -> {
                    AppDialog.notEnoughGemsDialog(this)
                }

                is MainViewModel.UnlockEvent.DailyLimit -> {
                    AppDialog.dailyLimitDialog(this)
                }

                is MainViewModel.UnlockEvent.RateLimit -> {
                    val message = event.message
                    val time = message.substringAfter("Please wait ", "")
                        .substringBefore(" before next claim", "")
                    val displayTime = if (time.isNotEmpty()) time else message
                    AppDialog.timeMegaLimitDialog(this, displayTime)
                }

                is MainViewModel.UnlockEvent.Error -> {
                    Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
                }
            }
            mainViewModel.clearUnlockEvent()
        }

        // Gems counter
        mainViewModel.userGems.observe(this) { gems ->
            Log.d(TAG, "User gems updated → $gems")
            binding.tvGems.text = gems?.toString() ?: "0"
            updateOfferProgressUI()
        }

        // Coins counter
        mainViewModel.userCoins.observe(this) { coins ->
            Log.d(TAG, "User coins updated → $coins")
            binding.tvCoins.text = coins?.toString() ?: "0"
        }

        // Stop swipe-refresh spinner once data load completes
        mainViewModel.homeDataLoading.observe(this) { isLoading ->
            if (!isLoading) binding.swipeRefreshLayout.isRefreshing = false
        }
        // Ad events
        mainViewModel.adEvent.observe(this) { event ->
            event ?: return@observe
            when (event) {
                is MainViewModel.AdEvent.AdCompleted -> {
                    val gems = mainViewModel.userGems.value ?: 0
                    val coins = mainViewModel.userCoins.value ?: 0

                    if (event.isMegaOffer) {
                        // Reward earned – reset offer progress and update UI
                        currentOfferProgress = 0
                        megaOfferClaimCount++
                        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putInt(KEY_OFFER_PROGRESS, currentOfferProgress)
                            .putInt(KEY_OFFER_CLAIM_COUNT, megaOfferClaimCount).apply()
                        updateOfferProgressUI()

                        Toast.makeText(
                            this, "🎉 Mega Offer Claimed! ${event.coinsEarned} Coins Reward.", Toast.LENGTH_LONG
                        ).show()
                        Log.d(TAG, "AdCompleted (MegaOffer) – offer reset, gems=$gems, coins=$coins")
                    } else {
                        Toast.makeText(
                            this, "🎉 Reward Earned! ${event.coinsEarned} Coins.", Toast.LENGTH_LONG
                        ).show()
                        Log.d(TAG, "AdCompleted (Regular) – gems=$gems, coins=$coins")
                    }
                }

                is MainViewModel.AdEvent.AdSkipped -> {
                    /* Toast.makeText(
                         this,
                         "Watch the full ad to earn your gem!",
                         Toast.LENGTH_SHORT
                     ).show()*/
                }

                is MainViewModel.AdEvent.AdFailedToShow -> {
                    //Toast.makeText(this, "Ad not available.", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "AdFailedToShow – no reward granted")
                }
            }
            mainViewModel.clearAdEvent()
        }
    }
}