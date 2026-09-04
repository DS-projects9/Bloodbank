package com.medkeen.plugins

import com.medkeen.LlmException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ErrorResponse(val ok: Boolean = false, val error: String)

private val jsonSerializer = Json { ignoreUnknownKeys = true }

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception(Throwable::class) { call: ApplicationCall, cause: Throwable ->
            val status = when (cause) {
                is IllegalArgumentException -> HttpStatusCode.BadRequest
                is SecurityException -> HttpStatusCode.Unauthorized
                is LlmException -> HttpStatusCode.ServiceUnavailable
                is IllegalStateException -> HttpStatusCode.Conflict
                else -> HttpStatusCode.InternalServerError
            }
            val message = when (cause) {
                is IllegalArgumentException, is SecurityException, is IllegalStateException ->
                    cause.message ?: "Request failed"
                else -> "Internal server error"
            }
            val body = jsonSerializer.encodeToString(ErrorResponse(error = message))
            call.respondText(body, ContentType.Application.Json, status)
        }
    }
}
