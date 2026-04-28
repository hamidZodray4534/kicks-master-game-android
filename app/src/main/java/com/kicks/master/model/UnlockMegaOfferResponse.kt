package com.kicks.master.model

import com.google.gson.annotations.SerializedName

data class UnlockMegaOfferResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val data: UnlockData?,
    val error: ErrorDetail?
)

data class UnlockData(
    @SerializedName("remaining_gems")
    val remainingGems: Int?,
    
    @SerializedName("last_unlock_time")
    val lastUnlockTime: String?
)


