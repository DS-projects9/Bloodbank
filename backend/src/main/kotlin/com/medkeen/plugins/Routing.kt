package com.medkeen.plugins

import com.medkeen.auth.AuthService
import com.medkeen.config.AppConfig
import com.medkeen.db.Database
import com.medkeen.seed.Seed
import com.medkeen.services.StorageService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.medkeen.routes.authRoutes
import com.medkeen.routes.userRoutes
import com.medkeen.routes.doctorRoutes
import com.medkeen.routes.appointmentRoutes
import com.medkeen.routes.bloodRoutes
import com.medkeen.routes.vaultRoutes
import com.medkeen.routes.aiRoutes
import com.medkeen.routes.cronRoutes
import kotlinx.coroutines.runBlocking

fun Application.configureRouting(config: AppConfig) {
    Database.init(config)
    AuthService.init(config)
    StorageService.init(config)
    if (config.seedDemoData) {
        val isDebug = System.getProperty("medkeen.debug") == "true" || System.getenv("MEDKEEN_DEBUG") == "true"
        if (isDebug) {
            runBlocking { Seed.run() }
        }
    }

    routing {
        route("/api/v1") {
            authRoutes()
            userRoutes()
            doctorRoutes()
            appointmentRoutes()
            bloodRoutes()
            vaultRoutes()
            aiRoutes()
            cronRoutes()
        }
    }
}
