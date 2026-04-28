package com.kicks.master.utills

object SessionEvent {

    @Volatile
    private var triggered = false

    fun notifyExpired() {
        if (triggered) return
        triggered = true
    }

    fun reset() {
        triggered = false
    }

    fun isExpired(): Boolean = triggered
}
