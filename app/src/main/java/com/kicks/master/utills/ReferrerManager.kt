package com.kicks.master.utills

import android.content.Context
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener

class ReferrerManager(private val context: Context) {

    data class ReferrerResult(
        val code: String?,
        val isPlayStoreRef: Boolean,
        val clickId: String? = null,
        val subId: String? = null
    )

    fun fetchReferralCode(onReferrerReceived: (ReferrerResult) -> Unit) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            val raw = referrerClient.installReferrer.installReferrer
                            Log.d("ReferrerManager", "raw referrer: $raw")

                            val playStoreRef = getQueryParam(raw, "ref")
                            val manualRef = getQueryParam(raw, "refer")
                            val clickId = getQueryParam(raw, "click_id")
                            val subId = getQueryParam(raw, "X-Sub-Id")

                            val result = when {
                                !playStoreRef.isNullOrBlank() -> ReferrerResult(
                                    playStoreRef,
                                    isPlayStoreRef = true,
                                    clickId = clickId,
                                    subId = subId
                                )

                                !manualRef.isNullOrBlank() -> ReferrerResult(
                                    manualRef,
                                    isPlayStoreRef = false,
                                    clickId = clickId,
                                    subId = subId
                                )

                                else -> ReferrerResult(
                                    null,
                                    isPlayStoreRef = false,
                                    clickId = clickId,
                                    subId = subId
                                )
                            }

                            Log.d("ReferrerManager", "parsed: $result")
                            onReferrerReceived(result)
                        } catch (e: Exception) {
                            Log.e("ReferrerManager", "getInstallReferrer failed: ${e.message}")
                            onReferrerReceived(ReferrerResult(null, false))
                        } finally {
                            try {
                                referrerClient.endConnection()
                            } catch (e: Exception) {
                            }
                        }
                    }

                    else -> {
                        Log.d("ReferrerManager", "Install referrer failed: $responseCode")
                        onReferrerReceived(ReferrerResult(null, false))
                        try {
                            referrerClient.endConnection()
                        } catch (e: Exception) {
                        }
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                Log.d("ReferrerManager", "Install referrer service disconnected")
                onReferrerReceived(ReferrerResult(null, false))
            }
        })
    }

    private fun getQueryParam(url: String?, key: String): String? {
        return url?.split("&")?.mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2 && parts[0] == key) parts[1] else null
        }?.firstOrNull()
    }
}
