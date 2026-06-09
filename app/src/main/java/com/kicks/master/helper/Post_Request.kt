package com.kicks.master.helper

import android.app.Activity
import android.util.Log
import com.kicks.master.Constant
import com.kicks.master.helper.apicall.RetrofitClient
import com.kicks.master.model.AdxUpdateRequest
import com.kicks.master.model.AdxUpdateResponse
import com.kicks.master.model.DTLimitResponse
import com.kicks.master.model.TrackAdsRequest
import com.kicks.master.utills.AppDialog.refresh_adx
import com.kicks.master.utills.DialogUtils
import com.kicks.master.utills.StringUtil
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
object Post_Request {
    private const val TAG = "Post_Request"
    private val apiService = RetrofitClient.apiService
    fun adx_update(activity: Activity) {
        val adxAccount = AppManager.getInstance(activity).getADX()
        val accountId = adxAccount?.id ?: 0
        
        // Ensure we have a valid numeric ID (e.g., 15) before calling API
        if (accountId == 0) {
            Log.d(TAG, "adx_update: No valid account ID found")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Passing the numeric ID as adx_item_id
                val request = AdxUpdateRequest(accountId.toString())
                val response = apiService.updateAdx(request)

                withContext(Dispatchers.Main) {
                    handleAdxResponse(activity, response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "adx_update failed: ${e.localizedMessage}")
            }
        }
    }

    private fun handleAdxResponse(activity: Activity, response: retrofit2.Response<AdxUpdateResponse>) {
        when (response.code()) {
            401 -> StringUtil.user_expire(activity)
            429 -> {
                val errorBody = response.errorBody()?.string()
                if (errorBody.isNullOrBlank()) return
                try {
                    val adxResponse = Gson().fromJson(errorBody, AdxUpdateResponse::class.java)
                    val error = adxResponse.error ?: return

                    Constant.setBoolean(activity, Constant.LIMIT_REACHED, error.limit_reached)
                    Constant.setBoolean(activity, Constant.SWITCH_ADX, error.switch_adx)

                    Log.d(TAG, "ADX 429: limit=${error.limit_reached}, switch=${error.switch_adx}")

                    if (error.limit_reached) {
                        refresh_adx(activity, error.switch_adx)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing ADX 429: ${e.message}")
                }
            }
            200 -> Log.d(TAG, "ADX update successful")
            else -> Log.d(TAG, "ADX update unexpected code: ${response.code()}")
        }
    }

    fun dt_update(activity: Activity) {
        val dtAccount = AppManager.getInstance(activity).getDigitalTurbineAdSetting()
        val accountId = dtAccount?.id ?: 0

        // Ensure we have a valid numeric ID (e.g., 12) before calling API
        if (accountId == 0) {
            Log.d(TAG, "dt_update: No valid account ID found")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Passing the numeric ID as dt_item_id
                val request = TrackAdsRequest(accountId.toString())
                val response = apiService.trackDtAds(request)

                withContext(Dispatchers.Main) {
                    handleDtResponse(activity, response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "dt_update failed: ${e.localizedMessage}")
            }
        }
    }

    private fun handleDtResponse(activity: Activity, response: retrofit2.Response<com.kicks.master.model.CoinCreditResponse>) {
        if (response.code() == 401) {
            StringUtil.user_expire(activity)
            return
        }

        if (response.code() == 429 || !response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            if (errorBody.isNullOrBlank()) return
            try {
                val dtResponse = Gson().fromJson(errorBody, DTLimitResponse::class.java)
                val error = dtResponse.error ?: return
                
                Log.d(TAG, "DT Limit Reached: $error")
                if (error.all_limit == 1) {
                    Constant.setBoolean(activity, Constant.IS_PRIMARY_PAUSED, true)
                } else if (error.limit_left == 0) {
                    Constant.setBoolean(activity, Constant.IS_PRIMARY_PAUSED, false)
                    AppManager.getInstance(activity).clearDigitalTurbineAdSetting()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing DT response: ${e.message}")
            }
        } else {
            Log.d(TAG, "DT tracking successful")
        }
    }

    fun creditMegaOffer(activity: Activity, offerId: String, clickId: String, subId: String,offerData: String) {
        DialogUtils.showLoading(activity)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.addMegaOffer(offerId,"mega_offer",clickId,subId,offerData)

                withContext(Dispatchers.Main) {
                    DialogUtils.hideLoading()
                    if (response.code() == 401) {
                        StringUtil.user_expire(activity)
                        return@withContext
                    }

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success && body.data != null) {
                            handleMegaOfferSuccess(activity, body)
                        } else {
                            handleMegaOfferFailure(activity, body)
                        }
                    } else {
                        Log.e(TAG, "Mega offer request failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    DialogUtils.hideLoading()
                    Log.e(TAG, "Mega offer crash avoided: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun handleMegaOfferSuccess(activity: Activity, body: com.kicks.master.model.CoinCreditResponse) {
        val data = body.data!!
        Constant.setString(activity, Constant.USER_COIN, data.user_coins.toString())
        Constant.setString(activity, Constant.MEGA_OFFER_DONE, "0")
        Log.d(TAG, "Mega offer credited: ${data.user_coins} coins")
    }

    private fun handleMegaOfferFailure(activity: Activity, body: com.kicks.master.model.CoinCreditResponse) {
        Log.d(TAG, "Mega offer failure: ${body.message}")
        if (body.statusCode == 429) {
            Constant.setString(activity, Constant.MEGA_OFFER_DONE, "1")
        }
    }
}