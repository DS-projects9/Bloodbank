package com.medvault.routes

import com.medvault.auth.AuthService
import com.medvault.db.FirestoreAdapter
import com.medvault.models.*
import com.medvault.models.requests.*
import com.medvault.plugins.requireAuth
import com.medvault.models.success
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

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
        val req = call.receive<RegisterRequest>()
        require(req.email.isNotBlank() && req.password.length >= 6) {
            "email and password (>=6 chars) are required"
        }

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
        req.role?.let { profile["role"] = it }

        FirestoreAdapter.setRaw("users", uid, profile)
        val token = AuthService.issueToken(uid, req.email, req.role, false)
        call.respond(success(TokenResponse(token, req.role, false, req.name, req.email, uid = uid)))
    }

    post("/auth/login") {
        val req = call.receive<LoginRequest>()
        require(req.email.isNotBlank() && req.password.isNotBlank()) { "email and password required" }

        val doc = FirestoreAdapter.queryRaw("users", listOf("email" to req.email), limit = 1).firstOrNull()
            ?: throw IllegalArgumentException("Invalid credentials")
        val data = doc.data
        val hash = data["passwordHash"] as? String
            ?: throw IllegalArgumentException("Invalid credentials")
        if (!AuthService.verifyPassword(req.password, hash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        val role = data["role"] as? String
        val verified = (data["consents"] as? Map<*, *>)?.get("dataStorage") == true
        val token = AuthService.issueToken(doc.id, req.email, role, verified)
        call.respond(success(TokenResponse(token, role, verified, data["name"] as? String, req.email, uid = doc.id)))
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
}
