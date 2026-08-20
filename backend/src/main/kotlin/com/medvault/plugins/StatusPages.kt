package com.medvault.plugins

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
                is IllegalStateException -> HttpStatusCode.Conflict
                else -> HttpStatusCode.InternalServerError
            }
            val body = jsonSerializer.encodeToString(ErrorResponse(error = cause.message ?: "Internal error"))
            call.respondText(body, ContentType.Application.Json, status)
        }
    }
}
