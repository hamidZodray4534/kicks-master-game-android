package com.kicks.master.helper

import android.content.Context
import android.content.Intent
import android.util.Log
import com.kicks.master.MainActivity

object NotificationRouter {
    fun getIntent(context: Context, data: org.json.JSONObject?): Intent {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val type = data?.optString(NotificationExtras.TYPE).orEmpty()
        val entityId = data?.optString(NotificationExtras.ENTITY_ID).orEmpty()

        Log.d("NotificationRouter", "Raw push converted to deep-link type=$type  entityId=$entityId")

        val path = when (type) {
            "super_offer" -> "offer/open"
            "read" -> "read/open"
            "video" -> "video/open"
            "game" -> "game/open"
            "payment_approved" -> "payment/open"
            else -> ""
        }

        if (path.isNotEmpty()) {
            val uriString = if (entityId.isNotBlank()) {
                "zodrewards://$path?item_id=$entityId"
            } else {
                "zodrewards://$path"
            }
            return Intent(context, MainActivity::class.java).apply {
                this.data = android.net.Uri.parse(uriString)
                this.flags = flags
            }
        }

        // Safe fallback
        Log.w("NotificationRouter", "Unknown type='$type' — opening MainActivity without deep-link")
        return context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { this.flags = flags }
            ?: Intent(context, MainActivity::class.java).apply { this.flags = flags }
    }
}