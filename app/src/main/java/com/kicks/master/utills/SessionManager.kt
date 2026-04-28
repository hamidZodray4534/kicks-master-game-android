package com.kicks.master.utills

import android.content.Context

object SessionManager {

    @Volatile
    private var isRefreshing = false

    lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun onSessionExpired() {
        if (isRefreshing) return
        isRefreshing = true
       // StringUtils.user_expire(appContext as Activity)
    }

    fun reset() {
        isRefreshing = false
    }
}
