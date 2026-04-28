package com.kicks.master.helper.apicall

/**
 * A generic class that holds a value with its loading status.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val statusCode: Int? = null, val errorCode: String? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
