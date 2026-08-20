package com.medvault.plugins

import com.medvault.config.FirebaseProvider
import com.google.cloud.firestore.Firestore
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

suspend fun RoutingCall.requireAuth(): AuthContext {
    val token = request.authorization()?.removePrefix("Bearer ")?.trim()
        ?: throw SecurityException("Missing Authorization header")

    val decoded = try {
        FirebaseProvider.auth().verifyIdToken(token)
    } catch (e: Exception) {
        throw SecurityException("Invalid or expired token")
    }

    // Prefer custom claims (set by /auth/setup-role). Fall back to the Firestore
    // users document — the Android client writes role/isOnboarded there directly
    // during onboarding, so this keeps the two layers in sync without forcing the
    // client to mint custom claims.
    var role = decoded.claims["role"] as? String
    var verified = decoded.claims["verified"] as? Boolean ?: false

    if (role == null) {
        try {
            val userDoc: Firestore = FirebaseProvider.firestore()
            val snap = userDoc.collection("users").document(decoded.uid).get().get()
            if (snap.exists()) {
                role = snap.getString("role")
                verified = snap.getBoolean("isOnboarded") ?: (role != null)
            }
        } catch (_: Exception) {
            // If Firestore is unreachable, keep null role (treated as PATIENT downstream).
        }
    }

    return AuthContext(
        uid = decoded.uid,
        email = decoded.email,
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
