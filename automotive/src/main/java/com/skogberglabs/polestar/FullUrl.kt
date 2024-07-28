package com.skogberglabs.polestar

import org.json.JSONException
import java.util.regex.Pattern

data class FullUrl(val proto: String, val hostAndPort: String, val uri: String) {
    private val host: String = hostAndPort.takeWhile { c -> c != ':' }
    private val protoAndHost = "$proto://$hostAndPort"
    val url = "$protoAndHost$uri"

    fun append(more: String) = copy(uri = this.uri + more)

    override fun toString(): String = url

    companion object {
        private val pattern = Pattern.compile("(.+)://([^/]+)(/?.*)")

        fun https(
            domain: String,
            uri: String,
        ): FullUrl = FullUrl("https", dropHttps(domain), uri)

        fun http(
            domain: String,
            uri: String,
        ): FullUrl = FullUrl("http", dropHttps(domain), uri)

        fun host(domain: String): FullUrl = FullUrl("https", dropHttps(domain), "")

        fun ws(
            domain: String,
            uri: String,
        ): FullUrl = FullUrl("ws", domain, uri)

        fun wss(
            domain: String,
            uri: String,
        ): FullUrl = FullUrl("wss", domain, uri)

        fun parse(input: String): FullUrl {
            return build(input)
                ?: throw JSONException("Value $input cannot be converted to FullUrl")
        }

        fun build(input: String): FullUrl? {
            val m = pattern.matcher(input)
            return if (m.find() && m.groupCount() == 3) {
                m.group(1)?.let { proto ->
                    m.group(2)?.let { host -> m.group(3)?.let { uri -> FullUrl(proto, host, uri) } }
                }
            } else {
                null
            }
        }

        private fun dropHttps(domain: String): String {
            val prefix = "https://"
            return if (domain.startsWith(prefix)) domain.drop(prefix.length) else domain
        }
    }
}
