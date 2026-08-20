package com.medvault

import com.medvault.config.AppConfig
import com.medvault.plugins.configureMonitoring
import com.medvault.plugins.configureRouting
import com.medvault.plugins.configureSerialization
import com.medvault.plugins.configureStatusPages
import com.medvault.services.SchedulerService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

fun main() {
    val config = AppConfig.load()
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        configureMonitoring()
        configureSerialization()
        configureStatusPages()
        configureRouting(config)
        environment.monitor.subscribe(ApplicationStopped) {
            SchedulerService.stop()
            appScope.coroutineContext.cancelChildren()
        }
    }.start(wait = false)

    SchedulerService.start(appScope)
    println("[MedVault] Server running on port ${config.port}")
    Thread.currentThread().join()
}
