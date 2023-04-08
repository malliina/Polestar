package com.skogberglabs.polestar

class Env {
    companion object {
        private const val BackendDomain = "api.boat-tracker.com"
        val baseUrl = FullUrl.https(BackendDomain, "")
//        private const val BackendDomain = "10.0.2.2:9000"
//        val baseUrl = FullUrl.http(BackendDomain, "")
        val socketsUrl = FullUrl.wss(BackendDomain, "/ws/updates")
    }
}
