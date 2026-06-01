package com.example.humsafar.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.humsafar.R

object BonusNotificationHelper {

    private const val CHANNEL_ID = "bonus_game"
    private const val NOTIFICATION_ID = 9001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Bonus challenges",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Surprise bonus game alerts while exploring heritage sites"
            }
        )
    }

    fun showBonusOffer(context: Context, targetNodeName: String, minutesLeft: Int) {
        ensureChannel(context.applicationContext)
        val ctx = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎁 Surprise bonus game!")
            .setContentText("Scan \"$targetNodeName\" within ${minutesLeft} min to play & win gems.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "A bonus challenge is live! Reach and scan \"$targetNodeName\" " +
                        "within $minutesLeft minutes to unlock a mini-game and earn bonus gems."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
    }
}
