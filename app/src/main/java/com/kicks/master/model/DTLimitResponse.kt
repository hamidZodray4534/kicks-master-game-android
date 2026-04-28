package com.kicks.master.model

data class DTLimitResponse(
    val success: Boolean?,
    val message: String?,
    val statusCode: Int?,
    val error: DTErrorData?
)
data class DTErrorData(
    val ads_count: Int?,
    val limit_left: Int?,
    val limit_reached: Boolean?,
    val switch_dt: Boolean?,
    val is_adx_all_limit_reached: Boolean?,
    val all_limit: Int?
)