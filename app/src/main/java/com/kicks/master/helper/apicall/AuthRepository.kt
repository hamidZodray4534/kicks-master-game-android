package com.kicks.master.helper.apicall

import com.kicks.master.model.LoginResponse
import com.kicks.master.model.SignupRequest

/**
 * Interface detailing exact Network interaction demands for Auth
 */
import com.kicks.master.model.SplashResponse

/**
 * Interface detailing exact Network interaction demands for Auth
 */
interface AuthRepository {
    suspend fun signUp(request: SignupRequest): Resource<LoginResponse>
    suspend fun getSplashData(): Resource<SplashResponse>
}


class AuthRepositoryImpl : BaseRepository(), AuthRepository {

    override suspend fun signUp(request: SignupRequest): Resource<LoginResponse> {
        return safeApiCall { RetrofitClient.authApiService.signUp(request) }
    }

    override suspend fun getSplashData(): Resource<SplashResponse> {
        return safeApiCall { RetrofitClient.apiService.getSplashData() }
    }
}
