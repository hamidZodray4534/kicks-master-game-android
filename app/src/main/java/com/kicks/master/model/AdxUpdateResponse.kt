package com.kicks.master.model

data class AdxUpdateResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val error: AdxError?
)

data class AdxError(
    val adx_impression: Int,
    val limit_reached: Boolean,
    val switch_adx: Boolean,
    val all_limit: Int,
)
