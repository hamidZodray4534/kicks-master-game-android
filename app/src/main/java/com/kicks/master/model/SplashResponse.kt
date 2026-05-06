package com.kicks.master.model

data class SplashResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val data: SplashData
)

data class SplashData(
    val adx_account: AdxAccount,
    val adx_task_history: List<Any>,
    val app_update: AppUpdate?,
    val app_maintenance: AppMaintenance?,
    val url: Url?
)

data class AdxAccount(
    val id: Int,
    val advertisement_type_id: Int,
    val app_id: String,
    val reward_unit_id: String,
    val native_unit_id: String,
    val banner_unit_id: String,
    val interstitial_unit_id: String,
    val max_impressions_limit: Int,
    val min_impressions_limit: Int,
    val max_interstitial_limit: Int,
    val min_interstitial_limit: Int
)

data class AppUpdate(
    val title: String,
    val subtitle: String,
    val version: String,
    val whatsnew: List<WhatsNew>
)

data class WhatsNew(
    val title: String,
    val description: String,
    val image: String
)

data class AppMaintenance(
    val title: String,
    val subtitle: String,
    val social_link: String,
    val social_link_label: String
)

data class Url(
    val privacy_policy: String,
    val term_condition: String
)
