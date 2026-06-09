package com.kicks.master.model

import com.google.gson.annotations.SerializedName

data class HomeResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val data: HomeData
)

data class HomeData(
    val userDetails: UserDetails,
    val megaOfferSettings: MegaOfferSettings,
    val adSettings: AdSettings,
    val adNetworkConfiguration: List<AdNetworkConfiguration>,
    val offer: Offer? = null
)

data class Offer(
    val id: Int,
    @SerializedName("offer_title")
    val offerTitle: String,
    @SerializedName("reward_type")
    val rewardType: String,
    @SerializedName("reward_count")
    val rewardCount: Int
)

data class UserDetails(
    val u_id: Int,
    val name: String,
    val email: String,
    val coins: Int,
    val gems: Int
)

data class MegaOfferSettings(
    val id: Int? = null,
    val slug: String? = null,
    val daily_limit: Int,
    val required_gems: Int,
    val win_gem_reward: Int,
    val win_coin_reward: Int,
    val media_size: Int,
    val interval: Long,
    val mega_offer_status: String
)

data class AdSettings(
    val adx: AdNetwork,

    @SerializedName("digital-turbine")
    val digitalTurbine: AdNetwork,

    @SerializedName("pub-max")
    val pubMax: AdNetwork,

    val yandex: AdNetwork,
    val vungle: AdNetwork,

    @SerializedName("cloud-x")
    val cloudX: AdNetwork? = null
)

data class AdNetwork(
    val id: Int,
    val advertisement_type_id: Int,
    val app_id: String,
    val api_key: String?,
    val reward_unit_id: String,
    val native_unit_id: String,
    val banner_unit_id: String,
    val interstitial_unit_id: String,
    val max_impressions_limit: Int,
    val min_impressions_limit: Int,

    val is_limit_reached: Boolean? = null,
    val limit_left: Int? = null,
    val is_adx_all_limit_reached: Boolean? = null,
    val is_dt_all_limit_reached: Boolean? = null,

    val advertisement_type: AdvertisementType
)

data class AdvertisementType(
    val id: Int,
    val title: String,
    val slug: String
)

data class AdNetworkConfiguration(
    val app_section: String,
    val ad_type: String,
    val primary_ad_network: String,
    val fallback_ad_networks: String
)

data class PlayGameResponse(
    val success: Boolean,
    val message: String,
    val statusCode: Int,
    val data: PlayGameData?
)

data class PlayGameData(
    val reward_amount: Int,
    val current_gems: Int
)
