package com.medvault.routes

import com.medvault.config.FirebaseProvider
import com.medvault.db.FirestoreAdapter
import com.medvault.models.*
import com.medvault.models.requests.*
import com.medvault.plugins.requireAuth
import com.medvault.utils.respondRaw
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
                val slotRef = FirebaseProvider.firestore().collection("slots").document(req.slotId)
                val slot = txn.get(slotRef).get()

                if (!slot.exists()) throw IllegalStateException("Slot not found")
                if (slot.getString("status") != "available") throw IllegalStateException("Slot not available")

                txn.update(slotRef, mapOf("status" to "locked", "lockedBy" to auth.uid, "lockedAt" to System.currentTimeMillis()))

                val apptRef = FirebaseProvider.firestore().collection("appointments").document()
                val appointment = mapOf(
                    "appointmentId" to apptRef.id,
                    "slotId" to req.slotId,
                    "patientUid" to auth.uid,
                    "doctorUid" to req.doctorUid,
                    "date" to slot.getString("date"),
                    "time" to slot.getString("startTime"),
                    "status" to "locked",
                    "patientNote" to req.patientNote,
                    "createdAt" to System.currentTimeMillis(),
                )
                txn.set(apptRef, appointment)
                appointment
            }

            call.respondRaw(result)
        }

        post("/confirm") {
            val auth = call.requireAuth()
            val req = call.receive<ConfirmAppointmentRequest>()

            FirestoreAdapter.runTransaction { txn ->
                val apptRef = FirebaseProvider.firestore().collection("appointments").document(req.appointmentId)
                val appt = txn.get(apptRef).get()

                if (!appt.exists()) throw IllegalStateException("Appointment not found")
                if (appt.getString("doctorUid") != auth.uid) throw SecurityException("Not your appointment")

                txn.update(apptRef, mapOf(
                    "status" to "confirmed",
                    "diagnosis" to req.diagnosis,
                    "followUpDate" to req.followUpDate,
                    "confirmedAt" to System.currentTimeMillis(),
                ))

                if (req.filesToShare.isNotEmpty()) {
                    val vaultRef = FirebaseProvider.firestore().collection("vault").document()
                    txn.set(vaultRef, mapOf(
                        "documentId" to vaultRef.id,
                        "ownerUid" to auth.uid,
                        "sharedWith" to listOf(appt.getString("patientUid")),
                        "fileNames" to req.filesToShare,
                        "createdAt" to System.currentTimeMillis(),
                        "expiresAt" to System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
                    ))
                }
            }

            call.respond(success(OkResponse()))
        }

        post("/cancel") {
            val auth = call.requireAuth()
            val req = call.receive<CancelAppointmentRequest>()

            FirestoreAdapter.runTransaction { txn ->
                val apptRef = FirebaseProvider.firestore().collection("appointments").document(req.appointmentId)
                val appt = txn.get(apptRef).get()

                if (!appt.exists()) throw IllegalStateException("Appointment not found")

                val slotId = appt.getString("slotId")
                if (slotId != null) {
                    val slotRef = FirebaseProvider.firestore().collection("slots").document(slotId)
                    txn.update(slotRef, mapOf("status" to "available", "lockedBy" to null, "lockedAt" to null))
                }

                txn.update(apptRef, mapOf(
                    "status" to "cancelled",
                    "cancelReason" to req.reason,
                    "cancelledAt" to System.currentTimeMillis(),
                ))
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
            call.respondRaw(appointments)
        }

        get("/{appointmentId}") {
            val auth = call.requireAuth()
            val apptId = call.parameters["appointmentId"]
                ?: throw IllegalArgumentException("Appointment ID required")

            val appt = FirestoreAdapter.get<Map<String, Any?>>("appointments", apptId)
                ?: throw IllegalArgumentException("Appointment not found")
            call.respondRaw(appt)
        }
    }
}
