package com.skogberglabs.polestar

object Utils {
    fun appId(label: String) = "${BuildConfig.APPLICATION_ID}.$label"
}
