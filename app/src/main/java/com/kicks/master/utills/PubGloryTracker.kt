package com.kicks.master.utills

import android.content.Context
import android.util.Log
import com.kicks.master.Constant
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.UUID

class PubGloryTracker(private val context: Context) {

    data class PubGloryRequest(
        val click_id: String,
        val sub_id: String,
        val app_id: String = "49253",
        val type: String,
        val tx_id: String
    )

    interface PubGloryApi {
        @POST("api/v1/conversion")
        suspend fun trackConversion(@Body body: PubGloryRequest): Response<ResponseBody>
    }

    private val api: PubGloryApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://track.pubglory.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PubGloryApi::class.java)
    }

    suspend fun trackInstall(clickId: String, subId: String) {
        val isTrackingDone = Constant.getBoolean(context, Constant.TRACKING_DONE)
        if (isTrackingDone) return

        val txId = UUID.randomUUID().toString()
        val request = PubGloryRequest(
            click_id = clickId,
            sub_id = subId,
            type = "install",
            tx_id = txId
        )

        try {
            val response = api.trackConversion(request)
            if (response.isSuccessful) {
                Log.d("PubGloryTracker", "Install tracking successful: ${response.body()?.string()}")
                // Save locally
                Constant.setString(context, Constant.CLICK_ID, clickId)
                Constant.setString(context, Constant.SUB_ID, subId)
                Constant.setBoolean(context, Constant.TRACKING_DONE, true)
            } else {
                Log.e("PubGloryTracker", "Install tracking failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("PubGloryTracker", "Install tracking error: ${e.message}")
        }
    }

    suspend fun trackRegistration() {
        val clickId = Constant.getString(context, Constant.CLICK_ID)
        val subId = Constant.getString(context, Constant.SUB_ID)

        if (clickId.isEmpty() || subId.isEmpty()) {
            Log.d("PubGloryTracker", "No clickId/subId found for registration tracking")
            return
        }

        val txId = UUID.randomUUID().toString()
        val request = PubGloryRequest(
            click_id = clickId,
            sub_id = subId,
            type = "registration",
            tx_id = txId
        )

        try {
            val response = api.trackConversion(request)
            if (response.isSuccessful) {
                Log.d("PubGloryTracker", "Registration tracking successful: ${response.body()?.string()}")
            } else {
                Log.e("PubGloryTracker", "Registration tracking failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("PubGloryTracker", "Registration tracking error: ${e.message}")
        }
    }
}
