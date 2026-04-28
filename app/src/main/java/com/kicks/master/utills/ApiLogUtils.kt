package com.kicks.master.utills

object ApiLogUtils {

    private const val TAG = "API_LOG"

  /*  private val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .create()
    }


    fun <T> logResponse(
        tag: String = TAG,
        response: Response<T>
    ) {
        try {
            if (response.isSuccessful) {
                val json = gson.toJson(response.body())
                Log.d(tag, "SUCCESS JSON:\n$json")
            } else {
                val errorJson = response.errorBody()?.string()
                Log.d(tag, "ERROR JSON:\n$errorJson")
            }
        } catch (e: Exception) {
            Log.d(tag, "Failed to log response", e)
        }
    }


    fun logRequest(tag: String = TAG, response: Response<*>) {
        val request = response.raw().request

        Log.d(tag, "URL: ${request.url}")
        Log.d(tag, "METHOD: ${request.method}")
        Log.d(tag, "HEADERS:\n${request.headers}")
    }


    fun logFailure(tag: String = TAG, t: Throwable) {
        Log.d(tag, "NETWORK FAILURE: ${t.localizedMessage}", t)
    }*/



}
