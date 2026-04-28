package com.kicks.master.helper.model

import com.google.gson.annotations.SerializedName

/**
 * Core user data model.
 *
 * Fields are kept minimal (Google sign-in only).
 * When your API is integrated, add server fields here (e.g. userId, gems, tier, etc.)
 * and update [AppManager.saveUser] / [AppManager.getUser] transparently.
 */
data class User(
    @SerializedName("name")   val name: String  = "",
    @SerializedName("email")  val email: String = "",
    @SerializedName("image")  val image: String = "",
    @SerializedName("token")  val token: String = "",

    // ── API fields (populated once back-end is live) ──────────────────────────
    @SerializedName("user_id")  val userId: String   = "",
    @SerializedName("gems")     val gems: Int         = 0,
    @SerializedName("coins")    val coins: Int        = 0,
    @SerializedName("refer")    val referCode: String = ""
)
