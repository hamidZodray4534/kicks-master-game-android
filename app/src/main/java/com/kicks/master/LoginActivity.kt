package com.kicks.master

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kicks.master.databinding.ActivityLoginBinding
import com.kicks.master.helper.AppManager
import com.kicks.master.helper.model.User
import com.kicks.master.login.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    // ── ViewBinding
    private lateinit var binding: ActivityLoginBinding

    // ── ViewModel ──
    private val viewModel: LoginViewModel by viewModels()

    // ── Google sign-in
    private lateinit var googleSignInClient: GoogleSignInClient

    // ── Managers ───
    private lateinit var appManager: AppManager

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            setSignInLoading(false)

            if (result.resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "Sign-in cancelled or failed – resultCode=${result.resultCode}")
                viewModel.onGoogleSignInCancelled()
                return@registerForActivityResult
            }
           val subId= Constant.getString(this, Constant.SUB_ID)

            val clickId = Constant.getString(this,Constant.CLICK_ID)

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.onGoogleAccountReceived(account,clickId,subId)
            } catch (e: ApiException) {
                Log.w(TAG, "GoogleSignIn ApiException statusCode=${e.statusCode}", e)
                viewModel.onGoogleSignInFailed(e.statusCode)
            }
        }


    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "Notification permission granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply window insets to prevent UI from overlapping the status bar and nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        appManager = AppManager.getInstance(this)
        


        initGoogleSignIn()
        observeViewModel()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
    }

    override fun onDestroy() {
        super.onDestroy()
    }




    private fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnGoogleSignIn.setOnClickListener {
            launchGoogleSignIn()
        }
    }


    private fun observeViewModel() {
        viewModel.loading.observe(this) { isLoading ->
            setSignInLoading(isLoading)
            if (isLoading) {
                com.kicks.master.utills.DialogUtils.showLoading(this)
            } else {
                com.kicks.master.utills.DialogUtils.hideLoading()
            }
        }

        viewModel.event.observe(this) { event ->
            event ?: return@observe
            when (event) {
                is LoginViewModel.LoginEvent.NavigateToMain -> {
                    persistUserAndNavigate(event.user)
                    viewModel.clearEvent()
                }
                is LoginViewModel.LoginEvent.ShowError -> {
                    Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearEvent()
                }
            }
        }
    }

    private fun launchGoogleSignIn() {
        setSignInLoading(true)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }


    private fun persistUserAndNavigate(user: User) {
        Log.d(TAG, "Persisting user → ${user.email}")

        appManager.saveUser(user)   // marks isLogin = true
        Log.d(TAG, "User saved to AppManager → navigating to MainActivity")

        navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }


    private fun setSignInLoading(loading: Boolean) {
        binding.btnGoogleSignIn.isEnabled = !loading
        binding.btnGoogleSignIn.alpha = if (loading) 0.6f else 1f
    }


    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
}