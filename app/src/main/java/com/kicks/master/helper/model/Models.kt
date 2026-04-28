package com.kicks.master.helper.model

import com.google.gson.annotations.SerializedName

/**
 * App-level settings from your back-end API.
 * Stubbed fields – extend when API contract is defined.
 */
data class Setting(
    @SerializedName("maintenance_mode") val maintenanceMode: Boolean = false,
    @SerializedName("version_code")     val versionCode: Int          = 0,
    @SerializedName("force_update")     val forceUpdate: Boolean       = false
)

/**
 * Extended setting data (loaded from splash API).
 */
data class SettingData(
    @SerializedName("maintenance_mode") val maintenanceMode: Boolean = false,
    @SerializedName("app_version")      val appVersion: String        = "",
    @SerializedName("force_update")     val forceUpdate: Boolean       = false,
    @SerializedName("min_redeem")       val minRedeem: Int             = 0
)

/**
 * AdX / Google Ad Manager account credentials.
 */
data class Adx_Account(
    @SerializedName("app_id")    val app_id:    String = "",
    @SerializedName("reward_id") val reward_id: String = "",
    @SerializedName("banner_id") val banner_id: String = "",
    @SerializedName("inter_id")  val inter_id:  String = "",
    @SerializedName("id")        val id:        Int    = 0
)

/**
 * Alias expected by AppManager (AdxAccount without underscore).
 */
typealias AdxAccount = Adx_Account

/**
 * Per-section ad configuration returned by the API.
 */
data class NewAdConfig(
    @SerializedName("app_section") val app_section: String = "",
    @SerializedName("ad_type")     val ad_type: String     = "",
    @SerializedName("ad_unit_id")  val ad_unit_id: String  = "",
    @SerializedName("enabled")     val enabled: Boolean    = false
)

/**
 * Ad network-specific settings (Vungle, Digital Turbine, etc.).
 */
data class AdSetting(
    @SerializedName("app_id")    val appId: String     = "",
    @SerializedName("placement") val placement: String = "",
    @SerializedName("enabled")   val enabled: Boolean  = false,
    @SerializedName("id")        val id:      Int      = 0
)
