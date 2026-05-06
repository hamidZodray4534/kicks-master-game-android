package com.kicks.master.helper.apicall


import com.kicks.master.model.AdxUpdateRequest
import com.kicks.master.model.AdxUpdateResponse
import com.kicks.master.model.CoinCreditResponse
import com.kicks.master.model.HomeResponse
import com.kicks.master.model.LoginResponse
import com.kicks.master.model.MegaOfferResponse
import com.kicks.master.model.PlayGameResponse
import com.kicks.master.model.SignupRequest
import com.kicks.master.model.SplashResponse
import com.kicks.master.model.TrackAdsRequest
import com.kicks.master.model.UnlockMegaOfferResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("offers")
    suspend fun getOffer(@Query("slug") slug: String = "mega-offer"): Response<MegaOfferResponse>

    @GET("home-page")
    suspend fun getHomeData(): Response<HomeResponse>
    @GET("splash-screen")
    suspend fun getSplashData(): Response<SplashResponse>

    @POST("play-game")
    suspend fun playGameCredit(@Query("slug") slug: String): Response<PlayGameResponse>

    @POST("login")
    suspend fun login(): Response<LoginResponse>

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignupRequest): Response<LoginResponse>

    @POST("adx/update")
    suspend fun updateAdx(@Body request: AdxUpdateRequest): Response<AdxUpdateResponse>

    @POST("track/dt-ads")
    suspend fun trackDtAds(@Body request: TrackAdsRequest): Response<CoinCreditResponse>

    @FormUrlEncoded
    @POST("offers/mega-offer/credit")
    suspend fun addMegaOffer(
        @Field("offer_id") offerId: String,
        @Field("slug") offerType: String,
        @Field("clickId") clickId: String,
        @Field("subId") subId: String
    ): Response<CoinCreditResponse>

    @FormUrlEncoded
    @POST("offers/mega-offer/unlock")
    suspend fun unlockMegaOffer(
        @Field("offer_id") offerId: String,
        @Field("slug") slug: String
    ): Response<UnlockMegaOfferResponse>
}