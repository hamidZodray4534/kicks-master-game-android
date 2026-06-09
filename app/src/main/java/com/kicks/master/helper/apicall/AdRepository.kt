package com.kicks.master.helper.apicall

import com.kicks.master.model.AdxUpdateRequest
import com.kicks.master.model.AdxUpdateResponse
import com.kicks.master.model.CoinCreditResponse
import com.kicks.master.model.TrackAdsRequest

class AdRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) : BaseRepository() {

    suspend fun updateAdx(request: AdxUpdateRequest): Resource<AdxUpdateResponse> {
        return safeApiCall { apiService.updateAdx(request) }
    }

    suspend fun trackDtAds(request: TrackAdsRequest): Resource<CoinCreditResponse> {
        return safeApiCall { apiService.trackDtAds(request) }
    }

    suspend fun creditMegaOffer(offerId: String, clickId: String, subId: String, offerData: String = ""): Resource<CoinCreditResponse> {
        return safeApiCall { apiService.addMegaOffer(offerId, "mega_offer", clickId, subId, offerData) }
    }
}
