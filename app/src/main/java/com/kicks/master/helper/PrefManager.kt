package com.kicks.master.helper


import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {

    private val pref: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = pref.edit()

    fun setString(key: String, value: String) {
        editor.putString(key, value).apply()
    }

    fun getString(key: String): String {
        return pref.getString(key, "") ?: ""
    }

    fun setBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    fun getBoolean(key: String): Boolean {
        return pref.getBoolean(key, false)
    }

    fun clearAll() {
        editor.clear().apply()
    }

    companion object {
        const val PREF_NAME = "burhanstore"
    }
}
