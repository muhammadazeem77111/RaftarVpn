package com.raftar.vpn.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.raftar.vpn.MainActivity

/**
 * Gaming Mode ki ongoing notification — live ping dikhati hai.
 * (VPN ka foreground service library ke :openvpn process me chalta hai,
 * is liye ye alag ongoing notification hai.)
 */
class GameNotification(private val context: Context) {

    private val nm = context.getSystemService(NotificationManager::class.java)
    private val channelId = "raftar_gaming"

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Gaming Mode", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun show(pingMs: Long?) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pingText = pingMs?.let { "$it ms" } ?: "…"
        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(Strings.t("gamingNotifTitle"))
            .setContentText("${Strings.t("gamingPing")}: $pingText")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        try {
            nm.notify(NOTIF_ID, n)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied — app ke andar ping phir bhi dikhta hai.
        }
    }

    fun cancel() {
        nm.cancel(NOTIF_ID)
    }

    private companion object {
        const val NOTIF_ID = 4242
    }
}
