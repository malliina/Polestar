package com.skogberglabs.polestar

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.skogberglabs.polestar.Utils.appAction
import com.skogberglabs.polestar.Utils.appId
import timber.log.Timber

class CarLocationService : Service() {
    private val intervalMillis = 5000L
    private val locationsPerBatch = 5
    private var started = false
    private lateinit var client: FusedLocationProviderClient
    private lateinit var pendingIntent: PendingIntent
    private lateinit var locationRequest: LocationRequest

    companion object {
        val LOCATIONS_CHANNEL = appId("channels.LOCATION")
        val STOP_LOCATIONS = appAction("STOP_LOCATIONS")

        fun createNotificationChannels(context: Context) {
            val channel = NotificationChannel(LOCATIONS_CHANNEL, "Car notifications", NotificationManager.IMPORTANCE_DEFAULT)
            val bootChannel = NotificationChannel(BootEventReceiver.BOOT_CHANNEL, "App boot notifications", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(channel, bootChannel)
            manager.createNotificationChannels(channels)
            val ids = channels.joinToString(separator = ", ") { it.id }
            Timber.i("Created notification channels $ids")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("Creating service...")
        val manager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NotificationIds.BOOT_ID)
        prepareLocations()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val describe = intent?.action ?: "no action"
        Timber.i("Got start command with $describe...")
        if (intent?.action == STOP_LOCATIONS) {
            stop()
        } else {
            if (!started) {
                if (applicationContext.isLocationGranted()) {
                    client.requestLocationUpdates(locationRequest, pendingIntent)
                    started = true
                    Timber.i("Started location service")
                }
            }
        }
        startForeground(NotificationIds.FOREGROUND_ID, notification())
        return START_STICKY
    }

    private fun prepareLocations() {
        val context = applicationContext
        client = LocationServices.getFusedLocationProviderClient(context)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(intervalMillis * locationsPerBatch) // batching, check the docs
            .build()
        val pi: PendingIntent by lazy {
            val intent = Intent(context, LocationUpdatesBroadcastReceiver::class.java).apply {
                action = LocationUpdatesBroadcastReceiver.ACTION_LOCATIONS
            }
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        }
        pendingIntent = pi
    }

    private fun notification(): Notification {
        val startAppIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(this.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(applicationContext, LOCATIONS_CHANNEL)
            .setContentTitle("Car-Tracker running")
            .setContentText("Enjoy the drive!")
            .setContentIntent(startAppIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    fun stop() {
        client.removeLocationUpdates(pendingIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Timber.i("Binding service...")
        return null
    }
}

fun Context.isLocationGranted(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun Context.isAllPermissionsGranted(): Boolean = PermissionContent.allPermissions.all { permission ->
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

fun Context.notGrantedPermissions(): List<String> = PermissionContent.allPermissions.filter { permission ->
    checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
}
