package com.kicks.master.utills

import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.security.SecureRandom
import java.util.UUID

object DeviceUtil {

    private const val PREF_NAME = "device_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    fun getDeviceId(context: Context): String {

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        prefs.getString(KEY_DEVICE_ID, null)?.let {
            return it
        }

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        val deviceId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            androidId
        } else {
            UUID.randomUUID().toString()
        }

        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()

        return deviceId
    }
    fun generateNonce(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }
}
