package com.kicks.master.model

data class CoinCreditResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val data: CoinData?,
    val error: ErrorDetail?
)

data class CoinData(
    val user_gems: Int,
    val user_coins: Int,
    val reward_amount: Int,
    val mega_offer_status: String
)
