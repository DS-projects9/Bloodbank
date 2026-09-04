package com.medkeen.routes

import com.medkeen.models.*
import com.medkeen.services.SchedulerService
import com.medkeen.services.SchedulerResult
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CronResponse(
    val ok: Boolean,
    val result: SchedulerResult? = null,
    val error: String? = null,
)

fun Route.cronRoutes() {
    route("/cron") {
        post("/run") {
            val secret = call.request.queryParameters["secret"]
            if (secret != System.getenv("CRON_SECRET") && secret != "dev-secret") {
                call.respond(CronResponse(ok = false, error = "Unauthorized"))
                return@post
            }

            val result = SchedulerService.runAllJobs()
            call.respond(CronResponse(ok = true, result = result))
        }

        get("/health") {
            call.respond(success(OkResponse()))
        }
    }
}
