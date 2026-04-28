package com.kicks.master.model

import com.google.gson.annotations.SerializedName
data class SignupRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("signup_type") val signupType: String,
    @SerializedName("language") val language: String = "eng",
    @SerializedName("u_id") val uId: String,
    @SerializedName("refer") val refer: String = "",
    @SerializedName("fcmtoken") val fcmtoken: String = ""
)
