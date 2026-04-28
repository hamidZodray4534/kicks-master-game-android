package com.kicks.master.utills

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.collections.iterator
import com.kicks.master.Constant
import com.kicks.master.LoginActivity


object StringUtil {

    private const val PREFS_NAME = "device_prefs"
    private const val KEY_FALLBACK_ID = "fallback_device_id"
    private const val TAG = "DeviceUtils"

    fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addresses = intf.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "Unavailable"
                    }
                }
            }
            "Unavailable"
        } catch (ex: Exception) {
            ex.printStackTrace()
            "Unavailable"
        }
    }

    fun get_uid(): String {
        val randomNumber = (10000000..99999999).random()
        return randomNumber.toString()
    }

    fun getCountryName(context: Context): String {
        return try {
            val teleMgr =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val countryIso = teleMgr.networkCountryIso
            if (countryIso.isNullOrEmpty()) "Unknown" else Locale("", countryIso).displayCountry
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }


    /**
     * Returns a unique device identifier.
     *
     * Uses ANDROID_ID, and if unavailable (very rare),
     * generates and saves a UUID as fallback.
     */
    fun getDeviceId(context: Context): String {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
            // 9774d56d682e549c is known to be a common buggy default ID on some old devices, ignore it
            Log.d(TAG, "Using ANDROID_ID: $androidId")
            return androidId
        }

        // If ANDROID_ID not available or invalid, fallback to generated UUID stored in SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var fallbackId = prefs.getString(KEY_FALLBACK_ID, null)

        if (fallbackId.isNullOrEmpty()) {
            fallbackId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_FALLBACK_ID, fallbackId).apply()
            Log.d(TAG, "Generated and saved fallback device ID: $fallbackId")
        } else {
            Log.d(TAG, "Using saved fallback device ID: $fallbackId")
        }

        return fallbackId
    }



    fun getFcmToken(onTokenReceived: (String) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onTokenReceived("")
                    return@addOnCompleteListener
                }
                val token = task.result ?: ""
                onTokenReceived(token)
            }
    }



    fun getStoreMb(activity: Activity): String {
        val path = activity.filesDir
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalSizeBytes = blockSize * totalBlocks
        val availableSizeBytes = blockSize * availableBlocks
        val usedSizeBytes = totalSizeBytes - availableSizeBytes
        val usedSizeMB = usedSizeBytes / (1024 * 1024)

        return usedSizeMB.toString()
    }


    fun extractYoutubeVideoId(url: String): String? {
        val uri = Uri.parse(url)
        return uri.getQueryParameter("v")
    }

    fun extractYoutubeVideoId2(url: String): String? {
        return try {
            val uri = Uri.parse(url)

            // Case 1: normal YouTube URL with v=VIDEO_ID
            uri.getQueryParameter("v")?.let { return it }

            // Case 2: short link format: youtu.be/VIDEO_ID
            if (uri.host.equals("youtu.be", ignoreCase = true)) {
                return uri.lastPathSegment
            }

            // Case 3: shorts URL: youtube.com/shorts/VIDEO_ID
            if (uri.pathSegments.contains("shorts")) {
                val index = uri.pathSegments.indexOf("shorts")
                if (index != -1 && index + 1 < uri.pathSegments.size) {
                    return uri.pathSegments[index + 1] // safely get ID after "shorts"
                }
            }

            // Case 4: embed URL: youtube.com/embed/VIDEO_ID
            if (uri.pathSegments.contains("embed")) {
                val index = uri.pathSegments.indexOf("embed")
                if (index != -1 && index + 1 < uri.pathSegments.size) {
                    return uri.pathSegments[index + 1]
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun formatDate(dateStr: String): String {

        return try {

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.ENGLISH)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Z means UTC
            val outputFormat = SimpleDateFormat("d MMM yyyy h:mm a", Locale.ENGLISH)
            outputFormat.timeZone = TimeZone.getDefault() // device timezone
            val date =
                inputFormat.parse(dateStr.replace("Z", "+0000")) // replace Z with +0000 for parsing
            outputFormat.format(date ?: return "Invalid Date")

        } catch (e: Exception) {
            e.printStackTrace()
            "Invalid Date"
        }

    }

    fun formatDate2(dateStr: String): String {

        return try {

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.ENGLISH)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // 'Z' মানে UTC
            val outputFormat = SimpleDateFormat("d MMM yyyy h:mm a", Locale.ENGLISH)
            outputFormat.timeZone = TimeZone.getDefault() // ডিভাইসের টাইমজোনে দেখাবে
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: return "Invalid Date")

        } catch (e: Exception) {
            e.printStackTrace()
            "Invalid Date"
        }

    }


    fun isInternetConnected(activity: Context): Boolean {
        val connectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }


    // Extract package name from Play Store URL
    fun extractPackageName(url: String): String? {
        val uri = Uri.parse(url)
        return uri.getQueryParameter("id")
    }

    fun isAppInstalled(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false
        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_ALL
            } else {
                0
            }
            context.packageManager.getPackageInfo(packageName, flags)
            Log.d("DEBUG", "App $packageName is installed")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("DEBUG", "App $packageName not found: ${e.message}")
            false
        } catch (e: Exception) {
            Log.d("DEBUG", "Unexpected error: ${e.message}")
            false
        }
    }


    // Open app in Play Store
    fun openInPlayStore(activity: Activity, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.setPackage("com.android.vending")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            activity.startActivity(intent)
        }
    }

    fun shareViaWhatsApp(activity: Activity, message: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            `package` = "com.whatsapp"
        }

        try {
            if (shareIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(shareIntent)
            } else {
               // custom_toast.showToast(activity, "WhatsApp not installed !!", true)
            }
        } catch (e: ActivityNotFoundException) {
            //custom_toast.showToast(activity, "WhatsApp not installed !!", true)
        } catch (e: Exception) {
            Log.d("WhatsAppShare", "Sharing failed", e)
        }
    }


    fun shareOnFacebook(context: Context, message: String) {
        val facebookUrl = "https://www.facebook.com/sharer/sharer.php?u=" + Uri.encode(message)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl))

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Facebook app not installed!", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText(context: Context, message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to share!", Toast.LENGTH_SHORT).show()
        }
    }

    fun getVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            -1
        }
    }




    fun getCountryCodeByIP(): String? {
        return try {
            val response = URL("http://ip-api.com/json").readText()
            val json = JSONObject(response)
            json.getString("countryCode") // ex: "BD"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun getCountryCode(context: Context): String {

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simCountry = tm.simCountryIso
        if (!simCountry.isNullOrEmpty()) {
            return simCountry.uppercase(Locale.US)
        }
        val networkCountry = tm.networkCountryIso
        if (!networkCountry.isNullOrEmpty()) {
            return networkCountry.uppercase(Locale.US)
        }
        return Locale.getDefault().country.uppercase(Locale.US)

    }

    /**
     * Handles 401 Unauthorized errors: clears all local data and redirects to LoginActivity.
     */
    fun user_expire(activity: Activity) {
        activity.runOnUiThread {
            // Clear all local preferences
            Constant.clearAll(activity)
            
            // Show message to user
            Toast.makeText(activity, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            
            // Redirect to LoginActivity and clear backstack
            val intent = Intent(activity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }
}