package com.skogberglabs.polestar.location

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
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.skogberglabs.polestar.BootEventReceiver
import com.skogberglabs.polestar.CarApp
import com.skogberglabs.polestar.NotificationIds
import com.skogberglabs.polestar.R
import com.skogberglabs.polestar.Utils.appAction
import com.skogberglabs.polestar.Utils.appId
import com.skogberglabs.polestar.ui.PermissionContent
import timber.log.Timber

/**
 * Foreground service.
 */
class CarLocationService : Service() {
    private val intervalMillis = 1000L
    private val locationsPerBatch = 5
    private var started = false
    private lateinit var client: FusedLocationProviderClient
    private lateinit var pendingIntent: PendingIntent
    private lateinit var locationRequest: LocationRequest
    val app: CarApp get() = application as CarApp

    companion object {
        val LOCATIONS_CHANNEL = appId("channels.LOCATION")
        val START_LOCATIONS = appAction("START_LOCATIONS")
        val STOP_LOCATIONS = appAction("STOP_LOCATIONS")

        const val Title = "title"
        const val Text = "text"

        fun createNotificationChannels(context: Context) {
            val channel =
                NotificationChannel(LOCATIONS_CHANNEL, "Car notifications", NotificationManager.IMPORTANCE_DEFAULT)
            val bootChannel =
                NotificationChannel(
                    BootEventReceiver.BOOT_CHANNEL,
                    "App boot notifications",
                    NotificationManager.IMPORTANCE_HIGH,
                )
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(channel, bootChannel)
            manager.createNotificationChannels(channels)
            val ids = channels.joinToString(separator = ", ") { it.id }
            Timber.i("Created notification channels $ids")
        }

        fun intent(
            context: Context,
            title: String,
            text: String,
        ): Intent =
            Intent(context, CarLocationService::class.java).apply {
                action = START_LOCATIONS
                putExtra(Title, title)
                putExtra(Text, text)
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
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        val describe = intent?.action ?: "no action"
        intent?.let { i ->
            when (i.action) {
                STOP_LOCATIONS -> {
                    stop()
                }
                START_LOCATIONS -> {
                    i.getStringExtra(Title)?.let { title ->
                        i.getStringExtra(Text)?.let { text ->
                            app.appService.signInSilently()
                            if (!started) {
                                if (applicationContext.isLocationGranted()) {
                                    client.requestLocationUpdates(locationRequest, pendingIntent)
                                    if (isForegroundServiceGranted()) {
                                        startForeground(
                                            NotificationIds.FOREGROUND_ID,
                                            notification(title, text),
                                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                                        )
                                        Timber.i(
                                            "Promoted location service to a foreground service, permissions granted ${isAllPermissionsGranted()}.",
                                        )
                                        started = true
                                    } else {
                                        Timber.i(
                                            "Foreground location service permission not granted, not promoting, other permissions granted ${isAllPermissionsGranted()}.",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Timber.i("Got start command with unexpected action $describe...")
                }
            }
        }
        return START_STICKY
    }

    private fun prepareLocations() {
        val context = applicationContext
        client = LocationServices.getFusedLocationProviderClient(context)
        locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                .setMaxUpdateDelayMillis(intervalMillis * locationsPerBatch) // batching, check the docs
                .build()
        val pi: PendingIntent by lazy {
            val intent =
                Intent(context, LocationUpdatesBroadcastReceiver::class.java).apply {
                    action = LocationUpdatesBroadcastReceiver.ACTION_LOCATIONS
                }
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        }
        pendingIntent = pi
    }

    private fun notification(
        title: String,
        text: String,
    ): Notification {
        val startAppIntent =
            PendingIntent.getActivity(
                this,
                0,
                packageManager.getLaunchIntentForPackage(this.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return Notification.Builder(applicationContext, LOCATIONS_CHANNEL)
            .setContentTitle(title)
            .setContentText(text)
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

fun Context.isForegroundServiceGranted(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

fun Context.isAllPermissionsGranted(): Boolean =
    PermissionContent.allPermissions.all { permission ->
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

fun Context.notGrantedPermissions(): List<String> =
    PermissionContent.allPermissions.filter { permission ->
        checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
    }
