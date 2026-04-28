package com.kicks.master.helper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson


import com.kicks.master.helper.model.User
import com.kicks.master.helper.model.NewAdConfig
import com.kicks.master.helper.model.SettingData
import com.kicks.master.helper.model.AdSetting
import com.kicks.master.helper.model.AdxAccount
import java.io.File


class AppManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: AppManager? = null

        fun getInstance(context: Context): AppManager {
            return instance ?: synchronized(this) {
                instance ?: AppManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val prefs: SharedPreferences by lazy {
        createEncryptedPrefs() ?: fallbackAfterClear()
    }
    private fun createEncryptedPrefs(): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                Const.PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("AppManager", "EncryptedSharedPreferences failed: ${e.message}")
            deleteEncryptedPrefsFile()
            null
        }
    }
    private fun fallbackAfterClear(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                Const.PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.d("AppManager", "Fallback also failed — using plain prefs: ${e.message}")
            context.getSharedPreferences(Const.PREF_NAME + "_plain", Context.MODE_PRIVATE)
        }
    }
   /* private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        EncryptedSharedPreferences.create(
            context,
            Const.PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }*/

    @Volatile
    private var cachedUser: User? = null

    @Volatile
    private var cachedSetting: SettingData? = null

    @Volatile
    private var cachedAdxAccount: AdxAccount? = null

    @Volatile
    private var cachedAdConfig: List<NewAdConfig>? = null

    @Volatile
    private var cachedDigitalTurbineAdSetting: AdSetting? = null

    @Volatile
    private var cachedVungleAdSetting: AdSetting? = null

    @Volatile
    private var cachedOffer: com.kicks.master.model.Offer? = null


    fun saveUser(user: User) {
        cachedUser = user
        prefs.edit().putString(Const.USER, Gson().toJson(user)).apply()

        setIsLogin(true)
    }


    fun getUser(): User? {
        if (cachedUser != null) return cachedUser
        val json = prefs.getString(Const.USER, null)
        cachedUser = json?.let {
            try { Gson().fromJson(it, User::class.java) } catch (e: Exception) { null }
        }
        return cachedUser
    }

    fun setIsLogin(isLogin: Boolean) {
        prefs.edit().putBoolean(Const.IS_LOGIN, isLogin).apply()
    }

    fun getIsLogin(): Boolean {
        return prefs.getBoolean(Const.IS_LOGIN, false)
    }


    fun saveSetting(setting: SettingData?) {
        cachedSetting = setting
        prefs.edit().putString(Const.SETTING, Gson().toJson(setting)).apply()
    }

    fun getSetting(): SettingData? {
        if (cachedSetting != null) return cachedSetting
        val json = prefs.getString(Const.SETTING, null)
        cachedSetting = json?.let {
            Gson().fromJson(it, SettingData::class.java)
        }
        return cachedSetting
    }


    fun saveAdConfig(adConfig: List<NewAdConfig>?) {
        cachedAdConfig = adConfig
        prefs.edit().putString(Const.ADS_CONFIG, Gson().toJson(adConfig)).apply()
    }

    fun getAdConfig(): List<NewAdConfig> {
        if (cachedAdConfig != null) return cachedAdConfig!!
        val json = prefs.getString(Const.ADS_CONFIG, null)
        cachedAdConfig = json?.let {
            try { Gson().fromJson(it, Array<NewAdConfig>::class.java).toList() } catch (e: Exception) { null }
        }
        return cachedAdConfig ?: emptyList()
    }

    fun getAdConfigForSection(section: String): NewAdConfig? {
        val configs = getAdConfig() ?: return null

        val target = normalizeSection(section)

        return configs.firstOrNull {
            normalizeSection(it.app_section) == target
        }
    }

    fun saveAdNetworkConfig(configs: List<com.kicks.master.model.AdNetworkConfiguration>?) {
        prefs.edit().putString("ad_network_configs", Gson().toJson(configs)).apply()
    }

    fun getAdNetworkConfigForSection(section: String): com.kicks.master.model.AdNetworkConfiguration? {
        val json = prefs.getString("ad_network_configs", null) ?: return null
        val configs = try {
            Gson().fromJson(json, Array<com.kicks.master.model.AdNetworkConfiguration>::class.java).toList()
        } catch (e: Exception) { return null }
        val target = normalizeSection(section)
        return configs.firstOrNull { normalizeSection(it.app_section) == target }
    }

    fun saveMegaOfferSettings(settings: com.kicks.master.model.MegaOfferSettings) {
        prefs.edit().putString("mega_offer_settings", Gson().toJson(settings)).apply()
    }

    fun getMegaOfferSettings(): com.kicks.master.model.MegaOfferSettings? {
        val json = prefs.getString("mega_offer_settings", null) ?: return null
        return try {
            Gson().fromJson(json, com.kicks.master.model.MegaOfferSettings::class.java)
        } catch (e: Exception) { null }
    }

    fun saveOffer(offer: com.kicks.master.model.Offer?) {
        cachedOffer = offer
        prefs.edit().putString("mega_offer_data", Gson().toJson(offer)).apply()
    }

    fun getOffer(): com.kicks.master.model.Offer? {
        if (cachedOffer != null) return cachedOffer
        val json = prefs.getString("mega_offer_data", null) ?: return null
        cachedOffer = try {
            Gson().fromJson(json, com.kicks.master.model.Offer::class.java)
        } catch (e: Exception) { null }
        return cachedOffer
    }


    private fun normalizeSection(value: String): String {
        return value.trim().lowercase().replace(" ", "").replace("_", "")
    }


    fun saveAdx(adxAccount: AdxAccount) {
        cachedAdxAccount = adxAccount
        prefs.edit().putString(Const.ADX_ACCOUNT, Gson().toJson(adxAccount)).apply()
    }

    fun getADX(): AdxAccount? {
        if (cachedAdxAccount != null) return cachedAdxAccount
        val json = prefs.getString(Const.ADX_ACCOUNT, null)
        cachedAdxAccount = json?.let {
            try { Gson().fromJson(it, AdxAccount::class.java) } catch (e: Exception) { null }
        }
        return cachedAdxAccount
    }

    fun saveVungleAdSetting(setting: AdSetting?) {
        cachedVungleAdSetting = setting
        prefs.edit().putString(Const.VUNGLE_AD_SETTING, Gson().toJson(setting)).apply()
    }

    fun getVungleAdSetting(): AdSetting? {
        if (cachedVungleAdSetting != null) return cachedVungleAdSetting
        val json = prefs.getString(Const.VUNGLE_AD_SETTING, null)
        cachedVungleAdSetting = json?.let {
            try { Gson().fromJson(it, AdSetting::class.java) } catch (e: Exception) { null }
        }
        return cachedVungleAdSetting
    }

    fun saveDigitalTurbineAdSetting(setting: AdSetting?) {
        cachedDigitalTurbineAdSetting = setting
        prefs.edit().putString(Const.DIGITAL_TURBINE_AD_SETTING, Gson().toJson(setting)).apply()
    }

    fun getDigitalTurbineAdSetting(): AdSetting? {
        if (cachedDigitalTurbineAdSetting != null) return cachedDigitalTurbineAdSetting
        val json = prefs.getString(Const.DIGITAL_TURBINE_AD_SETTING, null)
        cachedDigitalTurbineAdSetting = json?.let {
            try { Gson().fromJson(it, AdSetting::class.java) } catch (e: Exception) { null }
        }
        return cachedDigitalTurbineAdSetting
    }

    fun clearDigitalTurbineAdSetting() {
        cachedDigitalTurbineAdSetting = null
        prefs.edit().remove(Const.DIGITAL_TURBINE_AD_SETTING).remove(Const.VUNGLE_AD_SETTING)
            .apply()
    }


    fun clearUserData() {
        cachedUser = null
        cachedSetting = null
        cachedAdxAccount = null
        cachedAdConfig = null
        cachedDigitalTurbineAdSetting = null
        cachedVungleAdSetting = null
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            deleteEncryptedPrefsFile()
        }
    }


    fun logout() {
        cachedUser = null
        cachedSetting = null
        cachedAdxAccount = null
        try {
            prefs.edit().remove(Const.USER).remove(Const.IS_LOGIN).remove(Const.SETTING)
                .remove(Const.ADX_ACCOUNT).remove(Const.DIGITAL_TURBINE_AD_SETTING).apply()
        } catch (e: Exception) {
            deleteEncryptedPrefsFile()
        }
    }

    private fun deleteEncryptedPrefsFile() {
        try {
            // Delete corrupted prefs file
            val prefsFile = File(context.filesDir.parent, "shared_prefs/${Const.PREF_NAME}.xml")
            if (prefsFile.exists()) prefsFile.delete()

            // Delete master key from Android Keystore
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            }
            Log.d("AppManager", "Corrupted prefs cleared")
        } catch (e: Exception) {
            Log.e("AppManager", "Failed to clear prefs: ${e.message}")
        }
    }


}

