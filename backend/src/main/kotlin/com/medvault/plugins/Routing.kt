package com.medvault.plugins

import com.medvault.config.AppConfig
import com.medvault.config.FirebaseProvider
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

fun Application.configureRouting(config: AppConfig) {
    FirebaseProvider.initialize(config.projectId)

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
