package com.kicks.master.helper.apicall

import android.content.Context
import android.os.Build
import com.kicks.master.utills.DeviceUtil.generateNonce
import com.kicks.master.utills.DeviceUtil.getDeviceId
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.kicks.master.BuildConfig
import com.kicks.master.Constant
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.content.Intent


object RetrofitClient {

    private lateinit var apiServiceInternal: ApiService
    private lateinit var authApiServiceInternal: ApiService
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext

        val headerInterceptor = createHeaderInterceptor(applyAuth = true)
        val authHeaderInterceptor = createHeaderInterceptor(applyAuth = false)
        val loggingInterceptor = createLoggingInterceptor()


        val unauthorizedInterceptor = createUnauthorizedInterceptor()

        // ---------- NORMAL API CLIENT (with AuthInterceptor) ----------
        val apiOkHttpClient = OkHttpClient.Builder().addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).build()

        apiServiceInternal =
            Retrofit.Builder().baseUrl(BuildConfig.BASE_URL).client(apiOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(ApiService::class.java)

        // ---------- AUTH API CLIENT (NO AuthInterceptor) ----------
        val authOkHttpClient = OkHttpClient.Builder().addInterceptor(authHeaderInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()

        authApiServiceInternal =
            Retrofit.Builder().baseUrl(BuildConfig.BASE_URL).client(authOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(ApiService::class.java)
    }

    val apiService: ApiService get() = apiServiceInternal

    val authApiService: ApiService get() = authApiServiceInternal

    // ---------- Helpers ----------

    private fun createHeaderInterceptor(applyAuth: Boolean = true): Interceptor = Interceptor { chain ->
        val original = chain.request()

        val deviceId = getDeviceId(appContext)
        val accessToken = Constant.getString(appContext, Constant.USER_TOKEN)
        val googleToken = Constant.getString(appContext, Constant.GOOGLE_TOKEN)

        val security = RequestSecurityContext(
            timestamp = (System.currentTimeMillis() / 1000).toString(),
            nonce = UUID.randomUUID().toString().replace("-", "")


        )


        val userAgent = "KicksMaster/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"

        val apiNonce = generateNonce()



        val requestBuilder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("User-Agent", userAgent)
            .header("X-Device-Id", deviceId)
            .header("X-App-Version", BuildConfig.VERSION_NAME)
            .header("X-Platform", "android")
            .header("X-Request-Timestamp", security.timestamp)
            .header("X-Request-Nonce", security.nonce)
            .header("X-Api-Nonce", apiNonce)
            .header("social-auth-token", googleToken)



        if (applyAuth && !accessToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        chain.proceed(requestBuilder.build())
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

    private fun createUnauthorizedInterceptor(): Interceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post {
                val appManager = com.kicks.master.helper.AppManager.getInstance(appContext)
                appManager.logout()
                
                val intent = Intent(appContext, com.kicks.master.LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                appContext.startActivity(intent)
                android.widget.Toast.makeText(appContext, "Session expired, please login again", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        response
    }
}






