package com.kicks.master

import android.content.Context
import android.content.SharedPreferences
import com.kicks.master.helper.PrefManager


object Constant {

    //getAppSignature()

    // val BASE_URL: String = "https://test.zodwallet.app/"
    //val BASE_URL: String = "https://dev.zodwallet.app/"
    val DEV_KEY: String =
        "HCdtdsdsdsdsdsdsds2423423423vex4npUxPdO9jxTTCNt1PbNau5r7HC54353dsadas4324t1PbNau5r7"


    // Preference Keys
    const val USER_GEMS = "user_gems"
    const val TOTAL_SUPER_COMPLETED = "total_super_completed"
    const val USER_SELECTED_LAN = "user_selected_lan"
    const val USER_MEGA_GEMS = "usermega_gems"
    const val USER_GEMS_DONE = "user_gemsdone"
    const val USER_COIN = "user_coin"
    const val USER_TOKEN = "user_token"
    const val GOOGLE_TOKEN = "google_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val RUNNING_MB = "runninmb"
    const val VISIT_DONE = "visit_work"
    const val APP_DIALOG = "app_cdialog"
    const val PlayQuizDone = "play_quizw"
    const val YT_VIDEO = "yt_work"
    const val GAME_PLAY = "game_paly"
    const val isREADY_DELIVERY = "isrde"
    const val SUPER_OFFER_DONE = "super_offer_done"
    const val MEGA_OFFER_DONE = "mega_offer_done"
    const val SUPER_OFFER_ACTIVE = "super_offerac"
    const val MEGA_OFFER_ACTIVE = "mega_offerac"
    const val MEGA_OFFER_VALUE = "mega_offvale"
    const val MEGA_DAILY = "mega_daily"
    const val SUPPER_OFFER_VALUE = "super_offvale"
    const val IMG_BR_TIME = "img_br_time"
    const val REFER_DIALOG = "refer_dialogs"
    const val FAIR_BIRD_AD_DONE = "fair_bird_ad_done"
    const val FLASH_GATE_BACKUP = "flash_gate_backp"

    const val IMG_GAME_VALUE = "img_game_value"
    const val YT_WORKING = "yt_working"
    const val MEGA_LAST = "mega_last"
    const val SUPPER_LAST = "supper_last"

    const val CPX_OFFER_DATA = "cpx_offer_data"
    const val PUB_OFFER_DATA_NEW = "pub_offer_data"

    const val ADX_IMP_COUNT = "adx_impcount"
    const val SIGNUP_STATUS = "signup_status"

    const val CLICK_ID = "click_id"
    const val SUB_ID = "sub_id"
    const val TRACKING_DONE = "tracking_done"
    const val PRIVACY_POLICY = "privacy_policy"

    // Boolean Keys


    //app store data checking for value set 0 or 1
    const val APP_OPEN_FIRST_TIME = "flash_gate_backp"
    const val TURBO_OFFER_DATA = "turbooffer_data"
    const val TRACK_DATA = "track_data"
    const val REDEEMDATA = "redeemt_data"
    const val PUB_OFFER_DATA = "pub_offerdata"
    const val USER_REFER_TRACK = "urefer_track"
    const val FLASH_GATE_DATA = "flash_gatedata"
    const val PLAY_QUIZ_DATA = "play_quizdata"
    const val VISIT_DATA = "visit_data"
    const val OFFER_WALL_DATA = "offerwall_data"
    const val REDEEM_BANNER = "redeem_banner"
    const val REDEEM_METHOD = "redeem_method"
    const val STREAK_DATA = "streak_data"
    const val SOFFER_DATA = "s_offerdata"
    const val LAST_CLAIM_TIME = "last_claim_time"
    const val LIMIT_REACHED = "limit_reached"
    const val SWITCH_ADX = "switch_adx"
    const val BANNER_GAME_ID = "banner_game_id"
    const val PENDING_VIDEO_ID = "pending_video_id"
    const val PENDING_GAME_ID = "pending_game_id"
    const val IS_PRIMARY_PAUSED = "is_primary_paused"
    const val IS_SECONDARY_PAUSED = "is_secondary_paused"

    // Generic Pref Helper
    private fun getPrefs(context: Context, name: String): SharedPreferences {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    @Volatile
    private var prefManager: PrefManager? = null

    private fun getPrefManager(context: Context): PrefManager {
        return prefManager ?: synchronized(this) {
            prefManager ?: PrefManager(context.applicationContext).also { prefManager = it }
        }
    }

    fun setString(context: Context, key: String, value: String) {
        getPrefManager(context).setString(key, value)
    }

    fun getString(context: Context, key: String): String {
        return getPrefManager(context).getString(key)
    }


    fun setBoolean(context: Context, key: String, value: Boolean) {
        getPrefManager(context).setBoolean(key, value)
    }

    fun getBoolean(context: Context, key: String): Boolean {
        return getPrefManager(context).getBoolean(key)
    }

    fun clearAll(context: Context) {
        getPrefManager(context).clearAll()
    }


}
