package com.kicks.master.model

import com.google.gson.annotations.SerializedName

data class ErrorDetail(
    @SerializedName("ERROR_CODE")
    val errorCode: String
)
