package com.medvault.routes

import com.medvault.config.FirebaseProvider
import com.medvault.db.FirestoreAdapter
import com.medvault.models.*
import com.medvault.models.requests.*
import com.medvault.plugins.requireAuth
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class SetupRoleResponse(val role: String, val verified: Boolean)

@Serializable
data class AuthConfigResponse(
    val hasRole: Boolean,
    val hasConsents: Boolean,
    val role: String? = null,
    val name: String? = null,
    val email: String? = null,
)

fun Route.authRoutes() {
    post("/auth/setup-role") {
        val auth = call.requireAuth()
        val req = call.receive<SetupRoleRequest>()

        require(req.role in listOf("PATIENT", "DOCTOR", "BLOOD_BANK")) { "Invalid role" }

        val claims = mutableMapOf<String, Any>(
            "role" to req.role,
            "verified" to false,
        )
        FirebaseProvider.auth().setCustomUserClaims(auth.uid, claims)

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
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis(),
        )

        when (req.role) {
            "PATIENT" -> FirestoreAdapter.setRaw("users", auth.uid, profile)
            "DOCTOR" -> FirestoreAdapter.setRaw("users", auth.uid, profile)
            "BLOOD_BANK" -> {
                FirestoreAdapter.setRaw("users", auth.uid, profile)
                FirestoreAdapter.setRaw("blood_inventory", auth.uid, mapOf(
                    "uid" to auth.uid,
                    "bloodGroupUnits" to mapOf(
                        "A+" to 0, "A-" to 0, "B+" to 0, "B-" to 0,
                        "AB+" to 0, "AB-" to 0, "O+" to 0, "O-" to 0,
                    ),
                    "lastUpdated" to System.currentTimeMillis(),
                ))
            }
        }

        call.respond(success(SetupRoleResponse(role = req.role, verified = false)))
    }

    post("/auth/setup-consents") {
        val auth = call.requireAuth()
        val req = call.receive<SetupConsentsRequest>()

        FirestoreAdapter.setRaw("users", auth.uid, mapOf(
            "consents" to mapOf(
                "dataStorage" to req.dataStorage,
                "labResults" to req.labResults,
                "bloodDonation" to req.bloodDonation,
            ),
            "updatedAt" to System.currentTimeMillis(),
        ))

        FirebaseProvider.auth().setCustomUserClaims(auth.uid, mapOf("verified" to true))

        call.respond(success(VerifiedResponse(verified = true)))
    }

    get("/auth/config") {
        val auth = call.requireAuth()
        val user = FirestoreAdapter.get<Map<String, Any?>>("users", auth.uid)

        if (user == null) {
            call.respond(success(AuthConfigResponse(
                hasRole = false,
                hasConsents = false,
                role = null,
            )))
            return@get
        }

        val role = user["role"] as? String
        val consents = user["consents"] as? Map<*, *>
        val hasConsents = consents?.get("dataStorage") == true
        val hasRole = role != null

        call.respond(success(AuthConfigResponse(
            hasRole = hasRole,
            hasConsents = hasConsents,
            role = role,
            name = user["name"] as? String,
            email = user["email"] as? String,
        )))
    }
}
