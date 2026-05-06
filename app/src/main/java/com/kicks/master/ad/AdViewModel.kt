package com.kicks.master.ad

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kicks.master.helper.apicall.AdRepository
import com.kicks.master.helper.apicall.Resource
import com.kicks.master.model.AdxUpdateRequest
import com.kicks.master.model.AdxUpdateResponse
import com.kicks.master.model.CoinCreditResponse
import com.kicks.master.model.TrackAdsRequest
import kotlinx.coroutines.launch


class AdViewModel(
    private val repository: AdRepository = AdRepository()
) : ViewModel() {

    private val _adxUpdateResult = MutableLiveData<Resource<AdxUpdateResponse>?>()
    val adxUpdateResult: LiveData<Resource<AdxUpdateResponse>?> get() = _adxUpdateResult

    private val _trackAdsResult = MutableLiveData<Resource<CoinCreditResponse>?>()
    val trackAdsResult: LiveData<Resource<CoinCreditResponse>?> get() = _trackAdsResult

    private val _megaOfferResult = MutableLiveData<Resource<CoinCreditResponse>?>()
    val megaOfferResult: LiveData<Resource<CoinCreditResponse>?> get() = _megaOfferResult


    fun updateAdx(adxItemId: String) {
        _adxUpdateResult.value = Resource.Loading
        viewModelScope.launch {
            try {
                val request = AdxUpdateRequest(adxItemId)
                val response = repository.updateAdx(request)
                _adxUpdateResult.postValue(response)
            } catch (e: Exception) {
                _adxUpdateResult.postValue(Resource.Error(e.localizedMessage ?: "Unexpected error"))
            }
        }
    }

    fun trackDtAds(dtItemId: String) {
        _trackAdsResult.value = Resource.Loading
        viewModelScope.launch {
            try {
                val request = TrackAdsRequest(dtItemId)
                val response = repository.trackDtAds(request)
                _trackAdsResult.postValue(response)
            } catch (e: Exception) {
                _trackAdsResult.postValue(Resource.Error(e.localizedMessage ?: "Unexpected error"))
            }
        }
    }


    fun creditMegaOffer(offerId: String,clickId: String,subId: String) {
        _megaOfferResult.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = repository.creditMegaOffer(offerId,clickId,subId)
                _megaOfferResult.postValue(response)
            } catch (e: Exception) {
                _megaOfferResult.postValue(Resource.Error(e.localizedMessage ?: "Unexpected error"))
            }
        }
    }

    fun resetResults() {
        _adxUpdateResult.value = null
        _trackAdsResult.value = null
        _megaOfferResult.value = null
    }
}
