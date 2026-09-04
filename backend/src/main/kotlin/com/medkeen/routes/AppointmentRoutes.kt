package com.medkeen.routes

import com.medkeen.db.FirestoreAdapter
import com.medkeen.models.*
import com.medkeen.models.requests.*
import com.medkeen.plugins.requireAuth
import com.medkeen.services.AuditService
import com.medkeen.utils.respondRaw
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.appointmentRoutes() {
    route("/appointments") {
        post("/lock") {
            val auth = call.requireAuth()
            val req = call.receive<LockSlotRequest>()

            val result = FirestoreAdapter.runTransaction { txn ->
                val slot = txn.get("slots", req.slotId)
                    ?: throw IllegalStateException("Slot not found")
                if (slot.getString("status") != "available") {
                    throw IllegalStateException("Slot not available")
                }

                txn.update(
                    "slots", req.slotId,
                    mapOf(
                        "status" to "locked",
                        "lockedBy" to auth.uid,
                        "lockedAt" to System.currentTimeMillis(),
                    ),
                )

                val apptId = FirestoreAdapter.newId()
                val appointment = mapOf(
                    "appointmentId" to apptId,
                    "slotId" to req.slotId,
                    "patientUid" to auth.uid,
                    "doctorUid" to req.doctorUid,
                    "date" to slot.getString("date"),
                    "time" to slot.getString("startTime"),
                    "status" to "locked",
                    "patientNote" to req.patientNote,
                    "createdAt" to System.currentTimeMillis(),
                )
                txn.set("appointments", apptId, appointment)
                appointment
            }

            call.respondRaw(result)
        }

        post("/confirm") {
            val auth = call.requireAuth()
            val req = call.receive<ConfirmAppointmentRequest>()

            FirestoreAdapter.runTransaction { txn ->
                val appt = txn.get("appointments", req.appointmentId)
                    ?: throw IllegalStateException("Appointment not found")
                if (appt.getString("doctorUid") != auth.uid) {
                    throw SecurityException("Not your appointment")
                }

                val slotId = appt.getString("slotId")
                if (slotId != null) {
                    txn.update(
                        "slots", slotId,
                        mapOf("status" to "booked"),
                    )
                }

                txn.update(
                    "appointments", req.appointmentId,
                    mapOf(
                        "status" to "confirmed",
                        "diagnosis" to req.diagnosis,
                        "followUpDate" to req.followUpDate,
                        "confirmedAt" to System.currentTimeMillis(),
                    ),
                )

                if (req.filesToShare.isNotEmpty()) {
                    val vaultId = FirestoreAdapter.newId()
                    txn.set(
                        "vault", vaultId,
                        mapOf(
                            "documentId" to vaultId,
                            "ownerUid" to auth.uid,
                            "sharedWith" to listOf(appt.getString("patientUid")),
                            "fileNames" to req.filesToShare,
                            "createdAt" to System.currentTimeMillis(),
                            "expiresAt" to System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
                        ),
                    )
                }
            }

            call.respond(success(OkResponse()))
        }

        post("/{appointmentId}/complete") {
            val auth = call.requireAuth()
            val apptId = call.parameters["appointmentId"]
                ?: throw IllegalArgumentException("Appointment ID required")

            FirestoreAdapter.runTransaction { txn ->
                val appt = txn.get("appointments", apptId)
                    ?: throw IllegalStateException("Appointment not found")
                if (appt.getString("doctorUid") != auth.uid) {
                    throw SecurityException("Not your appointment")
                }

                val slotId = appt.getString("slotId")
                if (slotId != null) {
                    txn.update(
                        "slots", slotId,
                        mapOf("status" to "booked"),
                    )
                }

                txn.update(
                    "appointments", apptId,
                    mapOf(
                        "status" to "completed",
                        "completedAt" to System.currentTimeMillis(),
                    ),
                )
            }

            call.respond(success(OkResponse()))
        }

        post("/cancel") {
            val auth = call.requireAuth()
            val req = call.receive<CancelAppointmentRequest>()

            FirestoreAdapter.runTransaction { txn ->
                val appt = txn.get("appointments", req.appointmentId)
                    ?: throw IllegalStateException("Appointment not found")

                val slotId = appt.getString("slotId")
                if (slotId != null) {
                    txn.update(
                        "slots", slotId,
                        mapOf(
                            "status" to "available",
                            "lockedBy" to null,
                            "lockedAt" to null,
                        ),
                    )
                }

                txn.update(
                    "appointments", req.appointmentId,
                    mapOf(
                        "status" to "cancelled",
                        "cancelReason" to req.reason,
                        "cancelledAt" to System.currentTimeMillis(),
                    ),
                )
            }

            call.respond(success(OkResponse()))
        }

        get("/mine") {
            val auth = call.requireAuth()
            val role = auth.role

            val filters = when (role) {
                "DOCTOR" -> listOf("doctorUid" to auth.uid)
                else -> listOf("patientUid" to auth.uid)
            }

            val appointments = FirestoreAdapter.query<Map<String, Any?>>("appointments", filters, limit = 50)

            val enriched = if (role == "DOCTOR") {
                appointments.map { appt ->
                    val patientUid = appt["patientUid"] as? String ?: ""
                    val patient = FirestoreAdapter.get<Map<String, Any?>>("users", patientUid)
                    val sharedDocs = FirestoreAdapter.query<Map<String, Any?>>(
                        "vault",
                        listOf("ownerUid" to patientUid, "sharedWith" to listOf(auth.uid)),
                        limit = 50
                    )
                    val sharedForAppt = sharedDocs.filter { doc ->
                        (doc["appointmentId"] as? String) == appt["appointmentId"]
                    }
                    val dob = patient?.get("dob") as? String ?: ""
                    val age = if (dob.isNotEmpty()) {
                        try {
                            val birthDate = java.time.LocalDate.parse(dob)
                            java.time.Period.between(birthDate, java.time.LocalDate.now()).years
                        } catch (_: Exception) { 0 }
                    } else 0
                    appt.toMutableMap().apply {
                        put("patientName", patient?.get("name") ?: "Unknown")
                        put("age", age)
                        put("gender", patient?.get("gender") ?: "")
                        put("bloodGroup", patient?.get("bloodGroup") ?: "")
                        put("hasSharedRecords", sharedForAppt.isNotEmpty())
                        put("sharedRecordCount", sharedForAppt.size)
                    }
                }
            } else {
                appointments
            }

            call.respondRaw(enriched)
        }

        get("/{appointmentId}") {
            val auth = call.requireAuth()
            val apptId = call.parameters["appointmentId"]
                ?: throw IllegalArgumentException("Appointment ID required")

            val appt = FirestoreAdapter.get<Map<String, Any?>>("appointments", apptId)
                ?: throw IllegalArgumentException("Appointment not found")

            val patientUid = appt["patientUid"] as? String ?: ""
            val patient = FirestoreAdapter.get<Map<String, Any?>>("users", patientUid)

            val sharedDocs = FirestoreAdapter.query<Map<String, Any?>>(
                "vault",
                listOf("ownerUid" to patientUid, "sharedWith" to listOf(auth.uid)),
                limit = 50
            ).filter { doc ->
                (doc["appointmentId"] as? String) == apptId
            }

            val dob = patient?.get("dob") as? String ?: ""
            val age = if (dob.isNotEmpty()) {
                try {
                    val birthDate = java.time.LocalDate.parse(dob)
                    java.time.Period.between(birthDate, java.time.LocalDate.now()).years
                } catch (_: Exception) { 0 }
            } else 0

            val enriched = appt.toMutableMap().apply {
                put("patientName", patient?.get("name") ?: "Unknown Patient")
                put("age", age)
                put("gender", patient?.get("gender") ?: "")
                put("bloodGroup", patient?.get("bloodGroup") ?: "")
                put("phone", patient?.get("phone") ?: "")
                put("dob", dob)
                put("city", patient?.get("city") ?: "")
                put("email", patient?.get("email") ?: "")
                put("sharedDocCount", sharedDocs.size)
            }

            call.respondRaw(enriched)
        }

        post("/{appointmentId}/share") {
            val auth = call.requireAuth()
            val apptId = call.parameters["appointmentId"]
                ?: throw IllegalArgumentException("Appointment ID required")
            val req = call.receive<ShareFilesRequest>()

            val appt = FirestoreAdapter.get<Map<String, Any?>>("appointments", apptId)
                ?: throw IllegalStateException("Appointment not found")
            if ((appt["patientUid"] as? String) != auth.uid) {
                throw SecurityException("Only the patient can share files")
            }
            val doctorUid = appt["doctorUid"] as? String
                ?: throw IllegalStateException("Appointment has no doctor")

            val fileNames = req.fileNames.map { it.toString() }
            if (fileNames.isEmpty()) {
                throw IllegalArgumentException("No files selected")
            }

            val durationMinutes = req.durationMinutes?.takeIf { it > 0 } ?: 60

            val vaultId = FirestoreAdapter.newId()
            val doc = mapOf(
                "documentId" to vaultId,
                "ownerUid" to auth.uid,
                "sharedWith" to listOf(doctorUid),
                "appointmentId" to apptId,
                "fileNames" to fileNames,
                "durationMinutes" to durationMinutes,
                "wrappedKeys" to (req.wrappedKeys ?: emptyMap<String, String>()),
                "status" to "active",
                "createdAt" to System.currentTimeMillis(),
                "viewedAt" to 0L,
                "expiresAt" to 0L,
            )
            FirestoreAdapter.setRaw("vault", vaultId, doc)
            AuditService.log(
                "vault.share",
                auth.uid,
                vaultId,
                mapOf("appointmentId" to apptId, "doctorUid" to doctorUid, "fileCount" to fileNames.size),
            )
            call.respondRaw(doc)
        }

        get("/{appointmentId}/shares") {
            val auth = call.requireAuth()
            val apptId = call.parameters["appointmentId"]
                ?: throw IllegalArgumentException("Appointment ID required")

            val appt = FirestoreAdapter.get<Map<String, Any?>>("appointments", apptId)
                ?: throw IllegalStateException("Appointment not found")
            if ((appt["patientUid"] as? String) != auth.uid) {
                throw SecurityException("Only the patient can manage shares")
            }

            val docs = FirestoreAdapter.query<Map<String, Any?>>(
                "vault",
                listOf("appointmentId" to apptId),
                limit = 50
            ).filter {
                val owner = it["ownerUid"] as? String ?: ""
                owner == auth.uid
            }
            call.respondRaw(docs)
        }

        delete("/{appointmentId}/shares/{documentId}") {
            val auth = call.requireAuth()
            val apptId = call.parameters["appointmentId"]
                ?: throw IllegalArgumentException("Appointment ID required")
            val docId = call.parameters["documentId"]
                ?: throw IllegalArgumentException("Document ID required")

            val appt = FirestoreAdapter.get<Map<String, Any?>>("appointments", apptId)
                ?: throw IllegalStateException("Appointment not found")
            if ((appt["patientUid"] as? String) != auth.uid) {
                throw SecurityException("Only the patient can manage shares")
            }

            val doc = FirestoreAdapter.get<Map<String, Any?>>("vault", docId)
                ?: throw IllegalStateException("Share not found")
            if ((doc["ownerUid"] as? String) != auth.uid || (doc["appointmentId"] as? String) != apptId) {
                throw SecurityException("Not your share")
            }

            FirestoreAdapter.setRaw(
                "vault", docId,
                mapOf(
                    "status" to "revoked",
                    "sharedWith" to emptyList<String>(),
                    "expiresAt" to System.currentTimeMillis(),
                    "revokedAt" to System.currentTimeMillis(),
                ),
            )
            call.respond(success(OkResponse()))
        }

        post("/{appointmentId}/shares/{documentId}/extend") {
            val auth = call.requireAuth()
            val apptId = call.parameters["appointmentId"]
                ?: throw IllegalArgumentException("Appointment ID required")
            val docId = call.parameters["documentId"]
                ?: throw IllegalArgumentException("Document ID required")
            val req = call.receive<ExtendAccessRequest>()

            val appt = FirestoreAdapter.get<Map<String, Any?>>("appointments", apptId)
                ?: throw IllegalStateException("Appointment not found")
            if ((appt["patientUid"] as? String) != auth.uid) {
                throw SecurityException("Only the patient can manage shares")
            }

            val doc = FirestoreAdapter.get<Map<String, Any?>>("vault", docId)
                ?: throw IllegalStateException("Share not found")
            if ((doc["ownerUid"] as? String) != auth.uid || (doc["appointmentId"] as? String) != apptId) {
                throw SecurityException("Not your share")
            }
            val doctorUid = appt["doctorUid"] as? String
                ?: throw IllegalStateException("Appointment has no doctor")

            val durationMinutes = req.durationMinutes.takeIf { it > 0 }
                ?: throw IllegalArgumentException("Invalid duration")
            val expiresAt = System.currentTimeMillis() + durationMinutes * 60L * 1000

            val updated = mapOf(
                "status" to "active",
                "sharedWith" to listOf(doctorUid),
                "durationMinutes" to durationMinutes,
                "expiresAt" to expiresAt,
                "extendedAt" to System.currentTimeMillis(),
            )
            FirestoreAdapter.setRaw("vault", docId, updated)
            call.respond(success(FirestoreAdapter.get<Map<String, Any?>>("vault", docId) ?: updated))
        }

        get("/{appointmentId}/files") {
            val auth = call.requireAuth()
            val apptId = call.parameters["appointmentId"]
                ?: throw IllegalArgumentException("Appointment ID required")

            val appt = FirestoreAdapter.get<Map<String, Any?>>("appointments", apptId)
                ?: throw IllegalStateException("Appointment not found")
            val patientUid = appt["patientUid"] as? String ?: ""
            val doctorUid = appt["doctorUid"] as? String ?: ""
            if (auth.uid != patientUid && auth.uid != doctorUid) {
                throw SecurityException("Not authorized")
            }

            val docs = FirestoreAdapter.query<Map<String, Any?>>(
                "vault",
                listOf("appointmentId" to apptId),
                limit = 50
            ).filter {
                val sharedWith = it["sharedWith"] as? List<*> ?: emptyList<Any?>()
                val owner = it["ownerUid"] as? String ?: ""
                val status = it["status"] as? String ?: "active"
                status == "active" && (owner == auth.uid || sharedWith.contains(auth.uid))
            }
            call.respondRaw(docs)
        }
    }
}
