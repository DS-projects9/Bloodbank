package com.medkeen.routes

import com.medkeen.db.FirestoreAdapter
import com.medkeen.models.*
import com.medkeen.models.requests.*
import com.medkeen.plugins.requireAuth
import com.medkeen.services.StorageService
import com.medkeen.utils.respondRaw
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    route("/users") {
        get("/me") {
            val auth = call.requireAuth()
            val user = FirestoreAdapter.get<Map<String, Any?>>("users", auth.uid)
                ?: throw IllegalArgumentException("User not found")
            val safe = user.toMutableMap().apply {
                remove("passwordHash")
                remove("fcmToken")
            }
            call.respondRaw(safe)
        }

        patch("/me") {
            val auth = call.requireAuth()
            val req = call.receive<UpdateProfileRequest>()

            val updates = mutableMapOf<String, Any?>()
            req.name?.let { updates["name"] = it }
            req.phone?.let { updates["phone"] = it }
            req.bloodGroup?.let { updates["bloodGroup"] = it }
            req.city?.let { updates["city"] = it }
            req.dob?.let { updates["dob"] = it }
            req.hospitalName?.let { updates["hospitalName"] = it }
            req.hospitalAddress?.let { updates["hospitalAddress"] = it }
            req.specialization?.let { updates["specialization"] = it }
            req.licenseNumber?.let { updates["licenseNumber"] = it }
            req.bankName?.let { updates["bankName"] = it }
            req.bankAddress?.let { updates["bankAddress"] = it }
            req.bloodBankLicense?.let { updates["bloodBankLicense"] = it }

            if (updates.isNotEmpty()) {
                updates["updatedAt"] = System.currentTimeMillis()
                FirestoreAdapter.setRaw("users", auth.uid, updates)
            }

            call.respond(success(OkResponse()))
        }

        put("/contacts") {
            val auth = call.requireAuth()
            val req = call.receive<UpdateContactsRequest>()

            val contactsList = req.emergencyContacts.map { mapOf(
                "name" to it.name,
                "phone" to it.phone,
                "relationship" to it.relationship,
            ) }

            FirestoreAdapter.setRaw("users", auth.uid, mapOf(
                "emergencyContacts" to contactsList,
                "primaryContactUid" to req.primaryContactUid,
                "updatedAt" to System.currentTimeMillis(),
            ))

            call.respond(success(OkResponse()))
        }

        post("/fcm-token") {
            val auth = call.requireAuth()
            val req = call.receive<UpdateFcmTokenRequest>()

            FirestoreAdapter.setRaw("users", auth.uid, mapOf(
                "fcmToken" to req.fcmToken,
                "updatedAt" to System.currentTimeMillis(),
            ))

            call.respond(success(OkResponse()))
        }

        patch("/consents") {
            val auth = call.requireAuth()
            val req = call.receive<UpdateConsentsRequest>()

            FirestoreAdapter.setRaw("users", auth.uid, mapOf(
                "consents" to mapOf(
                    "dataStorage" to req.dataStorage,
                    "labResults" to req.labResults,
                    "bloodDonation" to req.bloodDonation,
                ),
                "updatedAt" to System.currentTimeMillis(),
            ))

            call.respond(success(OkResponse()))
        }

        // Delete only the reports/documents the user has stored in the vault.
        delete("/me/reports") {
            val auth = call.requireAuth()
            val docs = FirestoreAdapter.queryRaw("vault", listOf("ownerUid" to auth.uid), limit = 1000)
            docs.forEach { FirestoreAdapter.delete("vault", it.id) }
            call.respond(success(mapOf("deletedReports" to docs.size)))
        }

        // Delete the complete account and all related data across collections.
        delete("/me") {
            val auth = call.requireAuth()
            val uid = auth.uid

            deleteByField("vault", "ownerUid", uid)
            deleteByField("appointments", "patientUid", uid)
            deleteByField("appointments", "doctorUid", uid)
            deleteByField("blood_requests", "patientUid", uid)
            deleteByField("blood_donations", "donorUid", uid)
            deleteByField("schedule_config", "doctorUid", uid)
            deleteByField("slots", "doctorUid", uid)

            try { StorageService.deleteAllForUser(uid) } catch (_: Exception) { }

            FirestoreAdapter.delete("users", uid)

            call.respond(success(mapOf("deleted" to true)))
        }
    }
}

private suspend fun deleteByField(collection: String, field: String, value: String) {
    FirestoreAdapter.queryRaw(collection, listOf(field to value), limit = 2000)
        .forEach { FirestoreAdapter.delete(collection, it.id) }
}
