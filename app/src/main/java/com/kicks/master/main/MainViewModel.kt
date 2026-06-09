package com.kicks.master.main

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope
import com.kicks.master.SplashActivity
import kotlinx.coroutines.launch
import com.kicks.master.helper.apicall.Resource
import com.kicks.master.utills.DialogUtils

/**
 * ViewModel for MainActivity.
 */
class MainViewModel(
    private val repository: MainRepository = MainRepositoryImpl()
) : ViewModel() {

    private val TAG = "MainViewModel"

    companion object {
        // Public so SplashActivity writes to the SAME prefs file
        const val PREFS_NAME     = "rocket_main_prefs"
        const val KEY_USER_GEMS  = "user_gems_total"
        const val KEY_USER_COINS = "user_coins_total"
    }

    // ── Gems and Coins state ────────────────────────────────────────────────────────────

    private val _userGems = MutableLiveData(0)
    val userGems: LiveData<Int> get() = _userGems

    private val _userCoins = MutableLiveData(0)
    val userCoins: LiveData<Int> get() = _userCoins

    /** Load persisted gem and coin count from disk once per session. */
    fun loadGems(context: Context) {
        val prefs = getPrefs(context)
        _userGems.value = prefs.getInt(KEY_USER_GEMS, 0)
        _userCoins.value = prefs.getInt(KEY_USER_COINS, 0)
        Log.d(TAG, "Gems and coins loaded from prefs")
    }

    fun addReward(context: Context, gems: Int = 1, coins: Int = 0) {
        val currentGems = _userGems.value ?: 0
        val updatedGems = currentGems + gems
        _userGems.value = updatedGems

        val currentCoins = _userCoins.value ?: 0
        val updatedCoins = currentCoins + coins
        _userCoins.value = updatedCoins

        getPrefs(context).edit()
            .putInt(KEY_USER_GEMS, updatedGems)
            .putInt(KEY_USER_COINS, updatedCoins)
            .apply()

        Log.d(TAG, "Rewards updated → Gems: $updatedGems, Coins: $updatedCoins")
    }

    fun updateGems(context: Context, totalGems: Int) {
        _userGems.value = totalGems
        getPrefs(context).edit().putInt(KEY_USER_GEMS, totalGems).apply()
        Log.d(TAG, "Gems updated from backend → $totalGems")
    }

    fun updateCoins(context: Context, totalCoins: Int) {
        _userCoins.value = totalCoins
        getPrefs(context).edit().putInt(KEY_USER_COINS, totalCoins).apply()
        Log.d(TAG, "Coins updated from backend → $totalCoins")
    }

    /** Specifically handles the Mega Offer success to overwrite totals and signal claim completion */
    fun onMegaOfferClaimSuccess(context: Context, totalGems: Int, totalCoins: Int, coinsEarned: Int) {
        updateGems(context, totalGems)
        updateCoins(context, totalCoins)
        // Signal AdCompleted with isMegaOffer flag = true to trigger claim count increment in MainActivity
        if (coinsEarned > 0) {
            _adEvent.value = AdEvent.AdCompleted(0, coinsEarned, isMegaOffer = true)
        } else {
             Log.w(TAG, "Mega Offer claim reported success but zero coins earned.")
        }
    }

    // ── Ad event ──────────────────────────────────────────────────────────────

    sealed class AdEvent {
        data class AdCompleted(val gemsEarned: Int, val coinsEarned: Int, val isMegaOffer: Boolean = false) : AdEvent()   // ad shown & reward earned
        object AdSkipped    : AdEvent()   // ad dismissed without reward
        object AdFailedToShow : AdEvent() // ad not ready / show error
    }

    private val _adEvent = MutableLiveData<AdEvent?>()
    val adEvent: LiveData<AdEvent?> get() = _adEvent

    fun onRewardEarned(context: Context, gems: Int = 1, coins: Int = 0, isMegaOffer: Boolean = false) {
        addReward(context, gems, coins)
        _adEvent.value = AdEvent.AdCompleted(gems, coins, isMegaOffer)
    }

    fun onAdSkipped() {
        _adEvent.value = AdEvent.AdSkipped
    }

    fun onAdFailedToShow() {
        _adEvent.value = AdEvent.AdFailedToShow
    }

    private val _homeDataLoading = MutableLiveData(false)
    val homeDataLoading: LiveData<Boolean> get() = _homeDataLoading

    /**
     * Fetches latest home data from API and updates all local state.
     * @param forceRefresh if true, bypasses the in-session lock and always calls the API.
     */
    fun fetchHomeData(context: Context, forceRefresh: Boolean = false) {
        val prefs = getPrefs(context)
        val lastTime = prefs.getLong(SplashActivity.KEY_HOME_LAST_FETCH, 0L)
        val elapsed = System.currentTimeMillis() - lastTime

        // Skip if fetched within last 60 seconds AND not a forced refresh
        if (!forceRefresh && elapsed < 60_000L) {
            Log.d(TAG, "fetchHomeData: skipped (fetched ${elapsed / 1000}s ago)")
            return
        }

        if (_homeDataLoading.value == true) {
            Log.d(TAG, "fetchHomeData: already in-flight, skip")
            return
        }

        _homeDataLoading.value = true
        viewModelScope.launch {
            val result = repository.getHomeData()
            _homeDataLoading.postValue(false)

            if (result is Resource.Success) {
                val homeData = result.data.data
                val user = homeData.userDetails

                // Update live state
                _userCoins.postValue(user.coins)
                _userGems.postValue(user.gems)

                // Persist to prefs
                getPrefs(context).edit()
                    .putInt(KEY_USER_GEMS, user.gems)
                    .putInt(KEY_USER_COINS, user.coins)
                    .putLong(SplashActivity.KEY_HOME_LAST_FETCH, System.currentTimeMillis())
                    .apply()

                // Save everything to AppManager
                val adSettings = homeData.adSettings
                val appManager = com.kicks.master.helper.AppManager.getInstance(context)

                appManager.saveAdx(com.kicks.master.helper.model.AdxAccount(
                    app_id    = adSettings.adx?.app_id ?: "",
                    reward_id = adSettings.adx?.reward_unit_id ?: "",
                    banner_id = adSettings.adx?.banner_unit_id ?: "",
                    inter_id  = adSettings.adx?.interstitial_unit_id ?: "",
                    id        = adSettings.adx?.id ?: 0
                ))

                appManager.saveDigitalTurbineAdSetting(com.kicks.master.helper.model.AdSetting(
                    appId     = adSettings.digitalTurbine?.app_id ?: "",
                    placement = adSettings.digitalTurbine?.reward_unit_id ?: "",
                    enabled   = !(adSettings.digitalTurbine?.is_limit_reached ?: false),
                    id        = adSettings.digitalTurbine?.id ?: 0
                ))

                appManager.saveVungleAdSetting(com.kicks.master.helper.model.AdSetting(
                    appId     = adSettings.vungle?.app_id ?: "",
                    placement = adSettings.vungle?.reward_unit_id ?: "",
                    enabled   = true
                ))

                // Save CloudX ad settings
                appManager.saveCloudXAdSetting(com.kicks.master.helper.model.AdSetting(
                    appId     = adSettings.cloudX?.app_id ?: "",
                    placement = adSettings.cloudX?.reward_unit_id ?: "",
                    enabled   = true,
                    id        = adSettings.cloudX?.id ?: 0
                ))

                // Initialize CloudX SDK now that app_id is available
                com.kicks.master.helper.monetize.CloudX_Ad.initialize(context)

                appManager.saveAdNetworkConfig(homeData.adNetworkConfiguration)
                appManager.saveMegaOfferSettings(homeData.megaOfferSettings)
                appManager.saveOffer(homeData.offer)

                Log.d(TAG, "fetchHomeData OK → gems=${user.gems}, coins=${user.coins}, offerStatus=${homeData.megaOfferSettings.mega_offer_status}")
            } else if (result is Resource.Error) {
                Log.e(TAG, "fetchHomeData ERROR → ${result.statusCode}: ${result.message}")
            }
        }
    }

    fun clearAdEvent() {
        _adEvent.value = null
    }

    sealed class UnlockEvent {
        object Success : UnlockEvent()
        object AlreadyUnlocked : UnlockEvent()
        object NotEnoughGems : UnlockEvent()
        data class DailyLimit(val message: String) : UnlockEvent()
        data class RateLimit(val message: String) : UnlockEvent()
        data class Error(val message: String) : UnlockEvent()
    }

    private val _unlockEvent = MutableLiveData<UnlockEvent?>()
    val unlockEvent: LiveData<UnlockEvent?> get() = _unlockEvent

    fun unlockMegaOffer(context: Context, offerId: String, slug: String) {
      DialogUtils.showLoading(context as? android.app.Activity ?: return)
        viewModelScope.launch {
            val result = repository.unlockMegaOffer(offerId, slug)
           DialogUtils.hideLoading()
            
            if (result is Resource.Success) {
                // Deduct gems locally for instant UI update
                val appManager = com.kicks.master.helper.AppManager.getInstance(context)
                val settings = appManager.getMegaOfferSettings()
                if (settings != null) {
                    appManager.saveMegaOfferSettings(settings.copy(mega_offer_status = "ACTIVE"))
                }

                val reqGems = settings?.required_gems ?: 0
                val currentGems = _userGems.value ?: 0
                val updated = if (currentGems >= reqGems) currentGems - reqGems else 0
                _userGems.value = updated
                getPrefs(context).edit().putInt(KEY_USER_GEMS, updated).apply()

                _unlockEvent.value = UnlockEvent.Success
            } else if (result is Resource.Error) {
                val errorCode = result.errorCode
                val message = result.message
                
                when (errorCode) {
                    "ALREADY_UNLOCKED", "TASK_ALREADY_PROCESSED" -> {
                        _unlockEvent.value = UnlockEvent.AlreadyUnlocked
                    }
                    "TODAY_LIMIT_REACHED" -> {
                        _unlockEvent.value = UnlockEvent.DailyLimit(message)
                    }
                    else -> {
                        // Fallback to testing HTTP status codes if ERROR_CODE is missing
                        val code = result.statusCode
                        if (code == 409) {
                            _unlockEvent.value = UnlockEvent.AlreadyUnlocked
                        } else if (code == 400) {
                            _unlockEvent.value = UnlockEvent.NotEnoughGems
                        } else if (code == 429) {
                            if (message.contains("daily", ignoreCase = true)) {
                                _unlockEvent.value = UnlockEvent.DailyLimit(message)
                            } else {
                                _unlockEvent.value = UnlockEvent.RateLimit(message)
                            }
                        } else {
                            _unlockEvent.value = UnlockEvent.Error(message)
                        }
                    }
                }
            }
        }
    }

    fun clearUnlockEvent() {
        _unlockEvent.value = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
