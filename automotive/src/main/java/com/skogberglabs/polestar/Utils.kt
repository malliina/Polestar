package com.skogberglabs.polestar

object Utils {
    fun appId(label: String) = "${BuildConfig.APPLICATION_ID}.$label"

    fun appAction(label: String) = "${BuildConfig.APPLICATION_ID}.action.$label"
}

object NotificationIds {
    const val FOREGROUND_ID = 123
    const val BOOT_ID = 124
}
