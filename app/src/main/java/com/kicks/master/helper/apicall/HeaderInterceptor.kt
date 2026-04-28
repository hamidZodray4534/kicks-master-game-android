package com.kicks.master.helper.apicall

import android.content.Context
import com.kicks.master.BuildConfig
import com.kicks.master.Constant
import com.kicks.master.utills.DeviceUtil
import okhttp3.Interceptor
import okhttp3.Response

import java.util.UUID

class HeaderInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val original = chain.request()

        val token = Constant.getString(context, Constant.USER_TOKEN)
        val deviceId = DeviceUtil.getDeviceId(context)
        val appVersion = BuildConfig.VERSION_NAME
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val nonce = UUID.randomUUID().toString()

        val requestBuilder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("User-Agent", "MyApp/${appVersion} (Android)")
            .header("X-Device-Id", deviceId)
            .header("X-App-Version", appVersion)
            .header("X-Platform", "android")
            .header("X-Request-Timestamp", timestamp)
            .header("X-Request-Nonce", nonce)

        if (token.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
