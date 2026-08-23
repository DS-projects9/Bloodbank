package com.medvault.plugins

import com.medvault.auth.AuthService
import com.medvault.config.AppConfig
import com.medvault.db.Database
import com.medvault.seed.Seed
import com.medvault.services.StorageService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.medvault.routes.authRoutes
import com.medvault.routes.userRoutes
import com.medvault.routes.doctorRoutes
import com.medvault.routes.appointmentRoutes
import com.medvault.routes.bloodRoutes
import com.medvault.routes.vaultRoutes
import com.medvault.routes.aiRoutes
import com.medvault.routes.cronRoutes
import kotlinx.coroutines.runBlocking

fun Application.configureRouting(config: AppConfig) {
    Database.init(config)
    AuthService.init(config)
    StorageService.init(config)
    if (config.seedDemoData) {
        runBlocking { Seed.run() }
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
