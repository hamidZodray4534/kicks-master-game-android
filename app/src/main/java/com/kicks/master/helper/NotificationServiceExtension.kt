package com.kicks.master.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.util.Log
import com.onesignal.notifications.IDisplayableMutableNotification
import com.onesignal.notifications.INotificationReceivedEvent
import com.onesignal.notifications.INotificationServiceExtension

class NotificationServiceExtension : INotificationServiceExtension {

    companion object {
        private const val TAG = "NotifServiceExt"

        private const val CHANNEL_ID      = "zod_rewards_channel"
        private const val CHANNEL_NAME    = "Zod Rewards Notifications"
    }

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        val notification: IDisplayableMutableNotification = event.notification
        val context = event.context
        val additionalData = notification.additionalData
        ensureChannel(context)
        val imageUrl = additionalData
            ?.optString(NotificationExtras.IMAGE, null)
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?: additionalData?.optString("big_picture", null)
                ?.takeIf { it.isNotBlank() && it != "null" }

        Log.d(TAG, "onNotificationReceived: imageUrl=$imageUrl")

        event.preventDefault()

  /*      val bitmap: Bitmap? = if (!imageUrl.isNullOrBlank()) {
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load notification image: $imageUrl", e)
                null
            }
        } else null*/

       // val appIconBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
        notification.setExtender { builder ->
            builder
                .setChannelId(CHANNEL_ID)
                .setVibrate(longArrayOf(0L, 300L, 100L, 300L))
        }

      /*  Handler(Looper.getMainLooper()).post {
            try {
                playCustomSoundFor3Sec(context)
                vibratePhone(context)
            } catch (e: Exception) {
                Log.e(TAG, "Sound/vibration error", e)
            }
        }*/

        notification.display()

        Log.d(TAG, "Notification displayed with icon, image=$imageUrl")
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

      /*  val soundUri = Uri.parse(
            "android.resource://${context.packageName}/${R.raw.notification_sound}"
        )*/

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Push notifications for Zod Rewards"
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 300L, 100L, 300L)
           // setSound(soundUri, audioAttributes)
            enableLights(true)
        }

        manager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created: $CHANNEL_ID")
    }
}
