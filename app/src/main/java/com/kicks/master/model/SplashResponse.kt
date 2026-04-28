package com.kicks.master.model

data class SplashResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val data: SplashData
)

data class SplashData(
    val adx_account: AdxAccount,
    val adx_task_history: List<Any>
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
