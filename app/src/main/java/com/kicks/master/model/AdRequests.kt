package com.kicks.master.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for /adx/update
 */
data class AdxUpdateRequest(
    @SerializedName("adx_item_id")
    val adxItemId: String
)

/**
 * Request body for /track/dt-ads
 */
data class TrackAdsRequest(
    @SerializedName("dt_item_id")
    val dtItemId: String
)
