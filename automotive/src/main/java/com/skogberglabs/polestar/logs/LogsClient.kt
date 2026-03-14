package com.skogberglabs.polestar.logs

import com.skogberglabs.polestar.CarHttpClient
import com.skogberglabs.polestar.EnvConf
import com.skogberglabs.polestar.IdToken
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.TokenSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class LogsTokenSource(val http: CarHttpClient): TokenSource {
    override suspend fun fetchToken(): IdToken =
        http.post<TokenRequest, TokenResponse>("/sources/token", TokenRequest("polestar-android"), null).token
}

class LogsHttpClient(val http: CarHttpClient, val timber: TimberClient) {
    companion object {
        val instance = build()

        private fun build(): LogsHttpClient {
            val logger = TimberClient.instance
            Timber.plant(logger)
            val env = EnvConf.logs
            val tokenSource = LogsTokenSource(CarHttpClient(TokenSource.empty, env))
            return LogsHttpClient(CarHttpClient(tokenSource, env), logger)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun sendLogs(): Flow<Outcome<Published>> = timber.events().flatMapLatest { logs ->
        sendFlow(logs)
    }

    private fun sendFlow(logs: List<LogEvent>): Flow<Outcome<Published>> = flow {
        val outcome = try {
            val res = send(logs)
            Outcome.Success(res)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
        if (!outcome.isSuccess()) {
            delay(30.seconds)
            emitAll(sendFlow(logs))
        }
    }

    suspend fun send(events: List<LogEvent>): Published {
        return http.post<LogEvents, Published>("/sources/logs", LogEvents(events), null)
    }
}

class TimberClient: Timber.Tree() {
    companion object {
        val instance = TimberClient()
    }
    private val batch = mutableListOf<LogEvent>()

    fun events(): Flow<List<LogEvent>> = flow {
        while(true) {
            delay(1.seconds)
            val logs = rinse()
            if (logs.isNotEmpty()) {
                emit(logs)
            }
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val event = LogEvent(
            Instant.now(),
            message,
            tag ?: "android",
            Thread.currentThread().name,
            LogLevel.fromTimber(priority),
            t?.stackTraceToString()
        )
        add(event)
    }

    private fun add(event: LogEvent) {
        synchronized(this) {
            batch.add(event)
        }
    }

    private fun rinse(): List<LogEvent> {
        synchronized(this) {
            val ret = batch.toList()
            batch.clear()
            return ret
        }
    }
}