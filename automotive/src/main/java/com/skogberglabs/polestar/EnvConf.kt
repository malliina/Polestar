package com.skogberglabs.polestar

data class EnvConf(val domain: String, val secure: Boolean) {
    val baseUrl = FullUrl(if (secure) "https" else "http", domain, "")
    val socketsUrl = FullUrl(if (secure) "wss" else "ws", domain, "/ws/updates")

    companion object {
        private val prod = EnvConf("api.car-map.com", secure = true)
        private val dev = EnvConf("10.0.2.2:9000", secure = false)
        val current = prod
    }
}
