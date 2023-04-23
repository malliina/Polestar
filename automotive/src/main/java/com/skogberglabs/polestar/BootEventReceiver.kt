package com.skogberglabs.polestar

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class BootEventReceiver : BroadcastReceiver() {
    companion object {
        val BOOT_CHANNEL = Utils.appId("channels.BOOT")
    }
    override fun onReceive(context: Context, intent: Intent) {
        Timber.i("Boot event receiver received $intent")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.i("Boot completed.")
            val locationsIntent = PendingIntent.getForegroundService(
                context.applicationContext,
                0,
                Intent(context.applicationContext, LocationUpdatesBroadcastReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notification = Notification.Builder(context, BOOT_CHANNEL)
                .setContentTitle("Car-Tracker autostart")
                .setContentText("Start tracking?")
                .setContentIntent(locationsIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()
            val manager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NotificationIds.BOOT_ID, notification)
        }
    }
}
