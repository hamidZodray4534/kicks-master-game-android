package com.kicks.master.main

import com.kicks.master.helper.apicall.ApiService
import com.kicks.master.helper.apicall.BaseRepository
import com.kicks.master.helper.apicall.Resource
import com.kicks.master.helper.apicall.RetrofitClient
import com.kicks.master.model.HomeResponse

interface MainRepository {
    suspend fun getHomeData(offerData: String): Resource<HomeResponse>
    suspend fun unlockMegaOffer(offerId: String, slug: String): Resource<com.kicks.master.model.UnlockMegaOfferResponse>
}

class MainRepositoryImpl(
    private val apiService: ApiService = RetrofitClient.apiService
) : BaseRepository(), MainRepository {

    override suspend fun getHomeData(offerData: String): Resource<HomeResponse> {
        return safeApiCall { apiService.getHomeData(offerData) }
    }

    override suspend fun unlockMegaOffer(offerId: String, slug: String): Resource<com.kicks.master.model.UnlockMegaOfferResponse> {
        return safeApiCall { apiService.unlockMegaOffer(offerId, slug) }
    }
}
