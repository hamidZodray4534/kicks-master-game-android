package com.kicks.master.utills

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    private const val NULL = "null"

    @Volatile
    private var serverOffsetMs: Long = 0L

    private val utcTimeZone = TimeZone.getTimeZone("UTC")

    private val parserSdf = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        Locale.US
    ).apply {
        timeZone = utcTimeZone
        isLenient = false
    }

    private val isoGeneratorSdf = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        Locale.US
    ).apply {
        timeZone = utcTimeZone
        isLenient = false
    }

    /** Call this when API gives server current time */
    fun updateServerNow(serverNowUtc: String?) {
        val serverNowMs = parseUtcMillis(serverNowUtc) ?: return
        serverOffsetMs = serverNowMs - System.currentTimeMillis()
    }

    /** Always use this instead of System.currentTimeMillis() */
    private fun nowMs(): Long {
        return System.currentTimeMillis() + serverOffsetMs
    }

    /** Supports:
     * 2026-01-31T18:24:10.000Z
     * 2026-01-31T18:24:10.000000Z
     */
    private fun parseUtcMillis(raw: String?): Long? {
        if (raw.isNullOrBlank() || raw == NULL) return null

        val fixed = raw.replace(Regex("\\.(\\d{3})\\d+Z$"), ".$1Z")

        return try {
            parserSdf.parse(fixed)?.time
        } catch (_: Exception) {
            null
        }
    }

    fun shouldShowDialog(lastClaimTime: String?, limitMs: Long): Boolean {
        if (limitMs <= 0L) return false

        val last = parseUtcMillis(lastClaimTime) ?: return false
        val now = nowMs()

        val elapsed = (now - last).coerceAtLeast(0L)
        return elapsed < limitMs
    }

    fun getRemainingTimeTextMs(lastClaimTime: String?, limitMs: Long): String {
        if (limitMs <= 0L) return "0s"

        val last = parseUtcMillis(lastClaimTime) ?: return "0s"
        val now = nowMs()

        val elapsedMs = (now - last).coerceAtLeast(0L)
        val remainingMs = (limitMs - elapsedMs).coerceAtLeast(0L)

        return formatDuration(remainingMs)
    }

    fun elapsedSinceClaimMs(lastClaimTime: String?): Long {
        val last = parseUtcMillis(lastClaimTime) ?: return Long.MAX_VALUE
        val now = nowMs()
        return (now - last).coerceAtLeast(0L)
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun getUtcIsoTime(): String = isoGeneratorSdf.format(Date(nowMs()))

/*    fun isPhoneVerified(user: User?): Boolean {
        return user != null &&
                !user.phone_verify_at.isNullOrBlank() &&
                user.phone_verify_at != NULL &&
                !user.phone_number.isNullOrBlank() &&
                user.phone_number != NULL
    }*/
}
