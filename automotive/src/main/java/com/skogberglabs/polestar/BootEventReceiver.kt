package com.skogberglabs.polestar

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.skogberglabs.polestar.location.CarLocationService
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class BootEventReceiver : BroadcastReceiver() {
    companion object {
        val BOOT_CHANNEL = Utils.appId("channels.BOOT")
    }
    override fun onReceive(context: Context, intent: Intent) {
        Timber.i("Boot event receiver received $intent")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.i("Boot completed.")
            context.currentLangBlocking()?.let { lang ->
                val nlang = lang.notifications
                val locationsIntent = PendingIntent.getForegroundService(
                    context.applicationContext,
                    0,
                    CarLocationService.intent(context.applicationContext, nlang.autoStart, nlang.startTracking),
                    PendingIntent.FLAG_IMMUTABLE
                )
                val notification = Notification.Builder(context, BOOT_CHANNEL)
                    .setContentTitle(nlang.autoStart)
                    .setContentText(nlang.startTracking)
                    .setContentIntent(locationsIntent)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setOngoing(false)
                    .build()
                val manager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NotificationIds.BOOT_ID, notification)
            }
        }
    }
}

fun Context.currentLangBlocking(): CarLang? {
    val ctx = this
    return runBlocking { LocalDataSource(ctx).userPreferencesFlow().map { it.lang }.firstOrNull() }
}
