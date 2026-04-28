package com.kicks.master.utills

import android.content.Context
import android.util.Log

object TaskTimeUtils {

    private const val PREF_NAME = "task_time_pref"

    private const val KEY_START_TIME = "task_start_time"
    private const val KEY_END_TIME = "task_end_time"

    // -------------------------
    // START TIME
    // -------------------------
    fun saveStartTime(context: Context) {
        val time = System.currentTimeMillis()
        prefs(context).edit().putLong(KEY_START_TIME, time).apply()
        log("START_TIME saved = $time")
    }

    fun getStartTime(context: Context): Long {
        return prefs(context).getLong(KEY_START_TIME, 0L)
    }

    // -------------------------
    // END TIME
    // -------------------------
    fun saveEndTime(context: Context) {
        val time = System.currentTimeMillis()
        prefs(context).edit().putLong(KEY_END_TIME, time).apply()
        log("END_TIME saved = $time")
    }

    fun getEndTime(context: Context): Long {
        return prefs(context).getLong(KEY_END_TIME, 0L)
    }

    // -------------------------
    // ELAPSED TIME
    // -------------------------
    fun getElapsedSeconds(context: Context): Long {
        val start = getStartTime(context)
        val end = getEndTime(context)

        if (start == 0L || end == 0L) return 0L

        return (end - start) / 1000
    }

    fun getElapsedMillis(context: Context): Long {
        val start = getStartTime(context)
        val end = getEndTime(context)

        if (start == 0L || end == 0L) return 0L

        return end - start
    }

    // -------------------------
    // CLEAR (after reward)
    // -------------------------
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        log("Task time cleared")
    }

    // -------------------------
    // INTERNAL
    // -------------------------
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun log(msg: String) {
        Log.d("TASK_TIME_UTILS", msg)
    }
}
