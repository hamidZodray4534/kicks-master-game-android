package com.kicks.master.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val data: Data
)

data class Data(
    val signup_init: Boolean,
    val token: String,
    val refresh_token: String,
    val token_type: String,
    val user: User
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val language: String,
    val phone_number: String?,
    val coins: Int,
    val gems: Int,
    val phone_verified_at: String?,
    val email_verified_at: String?,
    val mega_offer_status: String,
    val fcm_token: String?,
    val profile_image: String?
)