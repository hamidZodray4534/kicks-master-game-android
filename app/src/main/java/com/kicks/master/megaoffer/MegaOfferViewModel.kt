package com.kicks.master.megaoffer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kicks.master.AppController
import com.kicks.master.Constant
import com.kicks.master.helper.apicall.RetrofitClient
import kotlinx.coroutines.launch

class MegaOfferViewModel : ViewModel() {

    sealed class Event {
        data class RewardEarned(val gems: Int, val coins: Int, val rewardAmount: Int = 0) : Event()
        object AdFailed : Event()
        object AdSkipped : Event()
        object AlreadyClaimed : Event()
        data class DailyLimit(val message: String) : Event()
        data class Error(val message: String) : Event()
    }

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> get() = _event

    fun onRewardEarned(rewardAmount: Int, coinReward: Int) {
        _event.value = Event.RewardEarned(gems = rewardAmount, coins = coinReward, rewardAmount = coinReward)
    }

    fun creditMegaOffer(offerId: String, offerType: String,clickId: String, subId: String) {
        viewModelScope.launch {
            try {

                Log.d("MegaOfferViewModel", "► Calling creditMegaOffer API: offer_id=$offerId, offer_type=$offerType")
                val response = RetrofitClient.apiService.addMegaOffer(offerId, offerType,clickId,subId)
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("MegaOfferViewModel", "► creditMegaOffer API Success: $body")
                    val data = body?.data
                    if (data != null) {
                        // Return updated balances AND the actual reward amount from API response
                        _event.value = Event.RewardEarned(
                            gems = data.user_gems, 
                            coins = data.user_coins, 
                            rewardAmount = data.reward_amount
                        )
                    } else {
                        Log.e("MegaOfferViewModel", "► creditMegaOffer API Data is null")
                        _event.value = Event.AdFailed
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("MegaOfferViewModel", "► creditMegaOffer API Failed: ${response.code()} Body: $errorBody")
                    
                    if (!errorBody.isNullOrEmpty()) {
                        try {
                            val errorJson = com.google.gson.Gson().fromJson(errorBody, com.google.gson.JsonObject::class.java)
                            val message = errorJson.get("message")?.asString ?: "Error"
                            val errorObj = errorJson.getAsJsonObject("error")
                            val errorCode = errorObj?.get("ERROR_CODE")?.asString
                            
                            when (errorCode) {
                                "TODAY_LIMIT_REACHED" -> _event.value = Event.DailyLimit(message)
                                "ALREADY_UNLOCKED", "TASK_ALREADY_PROCESSED" -> _event.value = Event.AlreadyClaimed
                                else -> _event.value = Event.Error(message)
                            }
                        } catch (e: Exception) {
                            Log.e("MegaOfferViewModel", "► Error parsing credit API error body", e)
                            _event.value = Event.AdFailed
                        }
                    } else {
                        _event.value = Event.AdFailed
                    }
                }
            } catch (e: Exception) {
                Log.e("MegaOfferViewModel", "► creditMegaOffer API Exception: ${e.message}")
                _event.value = Event.AdFailed
            }
        }
    }

    fun onAdFailed() {
        _event.value = Event.AdFailed
    }

    fun onAdSkipped() {
        _event.value = Event.AdSkipped
    }

    fun clearEvent() {
        _event.value = null
    }
}
