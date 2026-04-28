package com.kicks.master.model
import com.google.gson.annotations.SerializedName

data class MegaOfferResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val data: OfferData
)

data class OfferData(
    val id: Int,

    @SerializedName("offer_title")
    val offerTitle: String,

    @SerializedName("reward_type")
    val rewardType: String,

    @SerializedName("reward_count")
    val rewardCount: Int
)