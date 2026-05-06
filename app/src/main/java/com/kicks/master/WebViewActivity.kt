package com.kicks.master

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kicks.master.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply window insets to prevent UI from overlapping the status bar and nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        val url = intent.getStringExtra("url") ?: ""
        val title = intent.getStringExtra("title") ?: "Privacy Policy"

        binding.tvTitle.text = title

        binding.btnBack.setOnClickListener {
            handleBackAction()
        }

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            // Set a standard browser user agent to avoid being blocked/treated differently
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36"
            
            // Allow loading content from mixed sources (HTTP/HTTPS) if needed
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }

        if (url.isNotEmpty()) {
            // Remove X-Requested-With header which often causes servers to return 404/403 for security reasons
            val headers = HashMap<String, String>()
            headers["X-Requested-With"] = "" 
            binding.webView.loadUrl(url.trim(), headers)
        }
    }
    private fun handleBackAction() {
        finish()
        // Slide out to right, and main activity should slide in from left
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
