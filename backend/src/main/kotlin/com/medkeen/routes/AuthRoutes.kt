package com.medkeen.routes

import com.medkeen.auth.AuthService
import com.medkeen.auth.GoogleTokenVerifier
import com.medkeen.auth.RateLimiter
import com.medkeen.db.FirestoreAdapter
import com.medkeen.models.*
import com.medkeen.models.requests.*
import com.medkeen.plugins.requireAuth
import com.medkeen.models.success
import com.medkeen.services.AuditService
import com.medkeen.utils.respondRaw
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

private const val AUTH_RATE_LIMIT = 10
private const val AUTH_RATE_WINDOW_MS = 60_000L // 1 minute

private fun validatePasswordStrength(password: String) {
    require(password.length >= 8) { "Password must be at least 8 characters" }
    require(password.any { it.isUpperCase() }) { "Password must contain an uppercase letter" }
    require(password.any { it.isLowerCase() }) { "Password must contain a lowercase letter" }
    require(password.any { it.isDigit() }) { "Password must contain a digit" }
    require(password.any { !it.isLetterOrDigit() }) { "Password must contain a special character" }
}

@Serializable
data class SetupRoleResponse(val role: String, val verified: Boolean, val token: String? = null)

@Serializable
data class AuthConfigResponse(
    val hasRole: Boolean,
    val hasConsents: Boolean,
    val role: String? = null,
    val name: String? = null,
    val email: String? = null,
    val uid: String? = null,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val role: String? = null,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class GoogleAuthRequest(val idToken: String)

@Serializable
data class SetPublicKeyRequest(val publicKey: String)

@Serializable
data class TokenResponse(
    val token: String,
    val role: String?,
    val verified: Boolean,
    val name: String?,
    val email: String?,
    val uid: String? = null,
)

fun Route.authRoutes() {
    post("/auth/register") {
        val remoteIp = call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()
            ?: call.request.local.remoteAddress
        if (!RateLimiter.tryAcquire("register:$remoteIp", AUTH_RATE_LIMIT, AUTH_RATE_WINDOW_MS)) {
            throw IllegalArgumentException("Too many attempts. Please try again later.")
        }

        val req = call.receive<RegisterRequest>()
        require(req.email.isNotBlank()) { "Email is required" }
        validatePasswordStrength(req.password)

        if (FirestoreAdapter.queryRaw("users", listOf("email" to req.email), limit = 1).isNotEmpty()) {
            throw IllegalArgumentException("Email already registered")
        }

        val uid = FirestoreAdapter.newId()
        val profile = mutableMapOf<String, Any?>(
            "uid" to uid,
            "email" to req.email,
            "passwordHash" to AuthService.hashPassword(req.password),
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis(),
        )
        req.name?.let { profile["name"] = it }
        req.phone?.let { profile["phone"] = it }
        req.city?.let { profile["city"] = it }
        // Role cannot be self-assigned at registration; must use /auth/setup-role
        profile["role"] = "PATIENT"

        FirestoreAdapter.setRaw("users", uid, profile)
        val token = AuthService.issueToken(uid, req.email, "PATIENT", false)
        AuditService.log("auth.register", uid, details = mapOf("email" to req.email, "role" to "PATIENT"))
        call.respond(success(TokenResponse(token, "PATIENT", false, req.name, req.email, uid = uid)))
    }

    post("/auth/login") {
        val remoteIp = call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()
            ?: call.request.local.remoteAddress
        if (!RateLimiter.tryAcquire("login:$remoteIp", AUTH_RATE_LIMIT, AUTH_RATE_WINDOW_MS)) {
            throw IllegalArgumentException("Too many attempts. Please try again later.")
        }

        val req = call.receive<LoginRequest>()
        require(req.email.isNotBlank() && req.password.isNotBlank()) { "Email and password are required" }

        // Always use the same error message to prevent user enumeration
        val loginFailed = "Invalid email or password"

        val doc = FirestoreAdapter.queryRaw("users", listOf("email" to req.email), limit = 1).firstOrNull()
            ?: throw IllegalArgumentException(loginFailed)
        val data = doc.data
        val hash = data["passwordHash"] as? String
            ?: throw IllegalArgumentException(loginFailed)
        if (!AuthService.verifyPassword(req.password, hash)) {
            throw IllegalArgumentException(loginFailed)
        }

        val role = data["role"] as? String
        val verified = (data["consents"] as? Map<*, *>)?.get("dataStorage") == true
        val token = AuthService.issueToken(doc.id, req.email, role, verified)
        AuditService.log("auth.login", doc.id, details = mapOf("email" to req.email))
        call.respond(success(TokenResponse(token, role, verified, data["name"] as? String, req.email, uid = doc.id)))
    }

    post("/auth/google") {
        val remoteIp = call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()
            ?: call.request.local.remoteAddress
        if (!RateLimiter.tryAcquire("google:$remoteIp", AUTH_RATE_LIMIT, AUTH_RATE_WINDOW_MS)) {
            throw IllegalArgumentException("Too many attempts. Please try again later.")
        }

        val req = call.receive<GoogleAuthRequest>()
        require(req.idToken.isNotBlank()) { "Google ID token is required" }

        val payload = GoogleTokenVerifier.verify(req.idToken)
            ?: throw IllegalArgumentException("Invalid or expired Google token")

        val now = System.currentTimeMillis()
        val existing = FirestoreAdapter.queryRaw("users", listOf("googleSub" to (payload["sub"]!!)), limit = 1)
            .firstOrNull()
            ?: FirestoreAdapter.queryRaw("users", listOf("email" to (payload["email"]!!)), limit = 1).firstOrNull()

        val uid = existing?.id ?: FirestoreAdapter.newId()
        if (existing == null) {
            val profile = mutableMapOf<String, Any?>(
                "uid" to uid,
                "email" to payload["email"],
                "googleSub" to payload["sub"],
                "name" to payload["name"],
                "picture" to payload["picture"],
                "role" to "PATIENT",
                "createdAt" to now,
                "updatedAt" to now,
            )
            FirestoreAdapter.setRaw("users", uid, profile)
            AuditService.log("auth.register.google", uid, details = mapOf("email" to payload["email"]))
        }

        val user = FirestoreAdapter.getRaw("users", uid)
        val role = (user?.get("role") as? String) ?: "PATIENT"
        val verified = (user?.get("consents") as? Map<*, *>)?.get("dataStorage") == true
        val token = AuthService.issueToken(uid, payload["email"], role, verified)
        AuditService.log("auth.login.google", uid, details = mapOf("email" to payload["email"]))
        call.respond(
            success(
                TokenResponse(
                    token,
                    role,
                    verified,
                    payload["name"],
                    payload["email"],
                    uid = uid,
                ),
            ),
        )
    }

    post("/auth/setup-role") {
        val auth = call.requireAuth()
        val req = call.receive<SetupRoleRequest>()

        require(req.role in listOf("PATIENT", "DOCTOR", "BLOOD_BANK")) { "Invalid role" }

        val profile = mutableMapOf<String, Any?>(
            "uid" to auth.uid,
            "email" to auth.email,
            "role" to req.role,
            "name" to req.name,
            "phone" to req.phone,
            "dob" to req.dob,
            "bloodGroup" to req.bloodGroup,
            "city" to req.city,
            "hospitalName" to req.hospitalName,
            "hospitalAddress" to req.hospitalAddress,
            "specialization" to req.specialization,
            "licenseNumber" to req.licenseNumber,
            "bankName" to req.bankName,
            "bankAddress" to req.bankAddress,
            "bloodBankLicense" to req.bloodBankLicense,
            "consents" to mapOf(
                "dataStorage" to false,
                "labResults" to false,
                "bloodDonation" to false,
            ),
            "updatedAt" to System.currentTimeMillis(),
        )

        when (req.role) {
            "PATIENT" -> FirestoreAdapter.setRaw("users", auth.uid, profile)
            "DOCTOR" -> FirestoreAdapter.setRaw("users", auth.uid, profile)
            "BLOOD_BANK" -> {
                FirestoreAdapter.setRaw("users", auth.uid, profile)
                FirestoreAdapter.setRaw(
                    "blood_inventory",
                    auth.uid,
                    mapOf(
                        "uid" to auth.uid,
                        "bloodGroupUnits" to mapOf(
                            "A+" to 0, "A-" to 0, "B+" to 0, "B-" to 0,
                            "AB+" to 0, "AB-" to 0, "O+" to 0, "O-" to 0,
                        ),
                        "lastUpdated" to System.currentTimeMillis(),
                    ),
                )
            }
        }

        val token = AuthService.issueToken(auth.uid, auth.email, req.role, false)
        call.respond(success(SetupRoleResponse(role = req.role, verified = false, token = token)))
    }

    post("/auth/setup-consents") {
        val auth = call.requireAuth()
        val req = call.receive<SetupConsentsRequest>()

        FirestoreAdapter.setRaw(
            "users",
            auth.uid,
            mapOf(
                "consents" to mapOf(
                    "dataStorage" to req.dataStorage,
                    "labResults" to req.labResults,
                    "bloodDonation" to req.bloodDonation,
                ),
                "updatedAt" to System.currentTimeMillis(),
            ),
        )

        val user = FirestoreAdapter.getRaw("users", auth.uid)
        val role = user?.get("role") as? String
        val token = AuthService.issueToken(auth.uid, auth.email, role, true)
        AuditService.log("auth.consents", auth.uid, details = mapOf("dataStorage" to req.dataStorage))
        call.respond(success(VerifiedResponse(verified = true, token = token)))
    }

    get("/auth/config") {
        val auth = call.requireAuth()
        val user = FirestoreAdapter.get<Map<String, Any?>>("users", auth.uid)
            ?: run {
                call.respond(success(AuthConfigResponse(hasRole = false, hasConsents = false, role = null)))
                return@get
            }

        val role = user["role"] as? String
        val consents = user["consents"] as? Map<*, *>
        val hasConsents = consents?.get("dataStorage") == true
        call.respond(
            success(
                AuthConfigResponse(
                    hasRole = role != null,
                    hasConsents = hasConsents,
                    role = role,
                    name = user["name"] as? String,
                    email = user["email"] as? String,
                    uid = auth.uid,
                ),
            ),
        )
    }

    // Client-side encryption: store this user's public key (PEM) for envelope wrapping.
    post("/auth/public-key") {
        val auth = call.requireAuth()
        val req = call.receive<SetPublicKeyRequest>()
        require(req.publicKey.isNotBlank()) { "publicKey is required" }
        FirestoreAdapter.setRaw(
            "users",
            auth.uid,
            mapOf("publicKey" to req.publicKey, "publicKeyUpdatedAt" to System.currentTimeMillis()),
        )
        call.respond(success(mapOf("ok" to true)))
    }

    // Client-side encryption: fetch a recipient's public key for wrapping the DEK.
    get("/auth/public-key/{uid}") {
        val uid = call.parameters["uid"]
            ?: throw IllegalArgumentException("User ID required")
        val user = FirestoreAdapter.get<Map<String, Any?>>("users", uid)
        val publicKey = user?.get("publicKey") as? String
        if (publicKey.isNullOrBlank()) {
            call.respond(success(mapOf("publicKey" to null)))
            return@get
        }
        call.respondRaw(mapOf("publicKey" to publicKey))
    }
}
