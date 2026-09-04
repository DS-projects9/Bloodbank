package com.medkeen

import com.medkeen.config.AppConfig
import com.medkeen.plugins.configureMonitoring
import com.medkeen.plugins.configureRouting
import com.medkeen.plugins.configureSerialization
import com.medkeen.plugins.configureStatusPages
import com.medkeen.services.SchedulerService
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

    if (config.jwtSecret == "dev-only-insecure-change-me-in-production") {
        System.err.println(
            "[SECURITY WARNING] Using the built-in DEV JWT secret. Set JWT_SECRET in the environment for any non-local deployment.",
        )
    }
    if (System.getenv("MEDKEEN_REQUIRE_SECURE_SECRETS") == "true" &&
        config.jwtSecret == "dev-only-insecure-change-me-in-production"
    ) {
        throw IllegalStateException(
            "Refusing to start: insecure default secrets detected with MEDKEEN_REQUIRE_SECURE_SECRETS=true. Set JWT_SECRET.",
        )
    }

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
    println("[MedKeen] Server running on port ${config.port}")
    Thread.currentThread().join()
}
