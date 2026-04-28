package com.kicks.master.helper.apicall

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Base Repository that wrap API call with generic try-catch for safe network execution.
 */
abstract class BaseRepository {

    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Resource.Success(body)
                    } else {
                        Resource.Error("Response body is null", response.code())
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    var parsedMessage = "Error ${response.code()}: ${response.message()}"
                    var parsedErrorCode: String? = null

                    try {
                        if (!errorBody.isNullOrBlank()) {
                            val json = com.google.gson.Gson().fromJson(errorBody, com.google.gson.JsonObject::class.java)
                            
                            if (json.has("message")) {
                                var msg = json.get("message").asString
                                if (json.has("errors")) {
                                    val errorsObj = json.getAsJsonObject("errors")
                                    errorsObj.keySet().forEach { key ->
                                        val errs = errorsObj.getAsJsonArray(key)
                                        errs.forEach { msg += "\n- ${it.asString}" }
                                    }
                                }
                                parsedMessage = msg
                            } else {
                                parsedMessage = errorBody
                            }

                            if (json.has("error")) {
                                val errorObj = json.getAsJsonObject("error")
                                if (errorObj.has("ERROR_CODE")) {
                                    parsedErrorCode = errorObj.get("ERROR_CODE").asString
                                }
                            }
                        }
                    } catch (e: Exception) {
                        parsedMessage = if (!errorBody.isNullOrBlank()) errorBody else parsedMessage
                    }
                    Resource.Error(parsedMessage, response.code(), parsedErrorCode)
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}
