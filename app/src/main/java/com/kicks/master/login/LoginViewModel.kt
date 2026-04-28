package com.kicks.master.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.kicks.master.AppController
import com.kicks.master.Constant
import com.kicks.master.helper.AppManager
import com.kicks.master.helper.apicall.AuthRepository
import com.kicks.master.helper.apicall.AuthRepositoryImpl
import com.kicks.master.helper.apicall.Resource
import com.kicks.master.model.SignupRequest
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel for the Login screen.
 */
class LoginViewModel : ViewModel() {

    private val TAG = "LoginViewModel"
    private val authRepository: AuthRepository = AuthRepositoryImpl()

    // ── State ──────────────────────────────────────────────────────────────────

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _event = MutableLiveData<LoginEvent?>()
    val event: LiveData<LoginEvent?> get() = _event

    // ── Public API ────────────────────────────────────────────────────────────

    fun onGoogleAccountReceived(account: GoogleSignInAccount) {
        _loading.value = true
        Log.d(TAG, "Google account received → name=${account.displayName} email=${account.email}")

        val email = account.email
        val idToken = account.idToken?:""
        Constant.setString(AppController.instance.applicationContext, Constant.GOOGLE_TOKEN,idToken)


        if (email.isNullOrBlank()) {
            _loading.value = false
            _event.value = LoginEvent.ShowError("Google email not found. Please try again.")
            return
        }

        val userId = account.id ?: ""

        // Fetch FCM Token asynchronously before performing Signup
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val fcmToken = if (task.isSuccessful) task.result ?: "" else ""

            val request = SignupRequest(
                name = account.displayName ?: "",
                email = email,
                signupType = "app",
                uId = userId,
                refer = "",
                fcmtoken = fcmToken
            )

            viewModelScope.launch {
                val result = authRepository.signUp(request)
                _loading.value = false
                when (result) {
                    is Resource.Success -> {
                        val loginResponse = result.data
                        if (loginResponse.success) {
                            val apiUser = loginResponse.data.user
                            val mappedUser = com.kicks.master.helper.model.User(
                                name = apiUser.name,
                                email = apiUser.email,
                                image = account.photoUrl?.toString() ?: "",
                                token = loginResponse.data.token,
                                userId = apiUser.id.toString(),
                                gems = apiUser.gems,
                                coins = apiUser.coins,
                                referCode = ""
                            )
                            // Save token for header interceptor if needed immediately
                            try {
                                Constant.setString(AppController.instance.applicationContext, Constant.USER_TOKEN, loginResponse.data.token)
                            } catch (e: Exception) {}

                            // Call Splash API before navigating to ensure AdX credentials load
                            val splashResult = authRepository.getSplashData()
                            if (splashResult is Resource.Success && splashResult.data.success) {
                                val adx = splashResult.data.data.adx_account
                                val mappedAdx = com.kicks.master.helper.model.AdxAccount(
                                    app_id = adx.app_id,
                                    reward_id = adx.reward_unit_id,
                                    banner_id = adx.banner_unit_id,
                                    inter_id = adx.interstitial_unit_id
                                )
                                com.kicks.master.helper.AppManager.getInstance(AppController.instance.applicationContext).saveAdx(mappedAdx)
                            }

                            _event.value = LoginEvent.NavigateToMain(mappedUser)
                        } else {
                            _event.value = LoginEvent.ShowError(loginResponse.message)
                        }
                    }
                    is Resource.Error -> {
                        _event.value = LoginEvent.ShowError(result.message)
                    }
                    Resource.Loading -> {}
                }
            }
        }
    }

    fun onGoogleSignInCancelled() {
        Log.d(TAG, "Google sign-in cancelled by user")
        _event.value = LoginEvent.ShowError("Sign-in cancelled. Please try again.")
    }

    fun onGoogleSignInFailed(statusCode: Int) {
        Log.w(TAG, "Google sign-in failed → statusCode=$statusCode")
        _event.value = LoginEvent.ShowError("Google sign-in failed (code $statusCode). Please try again.")
    }

    fun clearEvent() {
        _event.value = null
    }

    sealed class LoginEvent {
        data class NavigateToMain(val user: com.kicks.master.helper.model.User) : LoginEvent()
        data class ShowError(val message: String) : LoginEvent()
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    data class GoogleUser(
        val name: String,
        val email: String,
        val photoUrl: String,
        val idToken: String
    )
}
