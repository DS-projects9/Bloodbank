package com.medkeen.plugins

import com.medkeen.auth.AuthService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingCall.requireAuth(): AuthContext {
    val token = request.authorization()?.removePrefix("Bearer ")?.trim()
        ?: throw SecurityException("Missing Authorization header")

    val decoded = try {
        AuthService.verifyToken(token)
    } catch (e: Exception) {
        throw SecurityException("Invalid or expired token")
    }

    val uid = decoded.subject ?: throw SecurityException("Invalid token")
    val email = decoded.getClaim("email").asString()?.takeIf { it.isNotBlank() }
    val role = decoded.getClaim("role").asString()?.takeIf { it.isNotBlank() }
    val verified = decoded.getClaim("verified").asBoolean() ?: false

    return AuthContext(
        uid = uid,
        email = email,
        role = role,
        verified = verified,
    )
}

data class AuthContext(
    val uid: String,
    val email: String?,
    val role: String?,
    val verified: Boolean,
)
