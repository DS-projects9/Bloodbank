package com.medvault.routes

import com.medvault.db.FirestoreAdapter
import com.medvault.models.*
import com.medvault.models.requests.*
import com.medvault.plugins.requireAuth
import com.medvault.utils.respondRaw
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bloodRoutes() {
    route("/blood-banks") {
        get("/search") {
            val auth = call.requireAuth()
            val bloodGroup = call.request.queryParameters["bloodGroup"]

            val banks = FirestoreAdapter.queryRaw("users", listOf("role" to "BLOOD_BANK"), limit = 200)
            val results = banks.mapNotNull { snap ->
                val d = (snap.data as? Map<String, Any?>) ?: return@mapNotNull null
                val id = snap.id
                val uid = (d["uid"] as? String)?.takeIf { it.isNotBlank() } ?: id
                val inv = FirestoreAdapter.getRaw("blood_inventory", uid)
                val rawUnits = (inv?.get("bloodGroupUnits") as? Map<*, *>) ?: emptyMap<String, Any>()
                val units = rawUnits.mapNotNull { (k, v) ->
                    (k as? String)?.let { it to ((v as? Number)?.toInt() ?: 0) }
                }.toMap()

                if (bloodGroup != null && (units[bloodGroup] ?: 0) <= 0) return@mapNotNull null

                mapOf(
                    "uid" to uid,
                    "name" to ((d["name"] as? String)?.takeIf { it.isNotBlank() }
                        ?: (d["bankName"] as? String) ?: (d["displayName"] as? String) ?: "Unknown Bank"),
                    "address" to (d["bankAddress"] as? String ?: d["hospitalAddress"] as? String ?: ""),
                    "bloodGroupUnits" to units
                )
            }
            call.respondRaw(results)
        }
    }

    route("/blood-requests") {
        post {
            val auth = call.requireAuth()
            val req = call.receive<BloodRequestCreate>()

            val reqId = FirestoreAdapter.newId()
            val request = mapOf(
                "requestId" to reqId,
                "patientUid" to auth.uid,
                "patientName" to req.patientName,
                "bloodGroup" to req.bloodGroup,
                "units" to req.units,
                "hospitalName" to req.hospitalName,
                "hospitalAddress" to req.hospitalAddress,
                "urgency" to req.urgency,
                "note" to req.note,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis(),
            )
            FirestoreAdapter.setRaw("blood_requests", reqId, request)
            call.respondRaw(request)
        }

        get("/mine") {
            val auth = call.requireAuth()
            val requests = FirestoreAdapter.query<Map<String, Any?>>(
                "blood_requests",
                listOf("patientUid" to auth.uid),
                limit = 50,
            )
            call.respondRaw(requests)
        }

        post("/fulfill") {
            val auth = call.requireAuth()
            val req = call.receive<BloodFulfillRequest>()

            FirestoreAdapter.runTransaction { txn ->
                val requestDoc = txn.get("blood_requests", req.requestId)
                    ?: throw IllegalStateException("Request not found")

                val inv = txn.get("blood_inventory", auth.uid)
                val units = (inv?.data?.get("bloodGroupUnits") as? Map<*, *>) ?: emptyMap<String, Any>()
                val bloodGroup = requestDoc.getString("bloodGroup") ?: ""
                val currentUnits = (units[bloodGroup] as? Number)?.toInt() ?: 0

                if (currentUnits < req.units) throw IllegalStateException("Insufficient units")

                txn.update(
                    "blood_inventory", auth.uid,
                    mapOf(
                        "bloodGroupUnits.$bloodGroup" to (currentUnits - req.units),
                        "lastUpdated" to System.currentTimeMillis(),
                    ),
                )

                val grantId = FirestoreAdapter.newId()
                txn.set(
                    "blood_grants", grantId,
                    mapOf(
                        "grantId" to grantId,
                        "requestId" to req.requestId,
                        "bloodBankUid" to auth.uid,
                        "bloodGroup" to bloodGroup,
                        "units" to req.units,
                        "createdAt" to System.currentTimeMillis(),
                    ),
                )

                txn.update(
                    "blood_requests", req.requestId,
                    mapOf(
                        "status" to "fulfilled",
                        "fulfilledAt" to System.currentTimeMillis(),
                    ),
                )
            }

            call.respond(success(OkResponse()))
        }

        get("/nearby") {
            val auth = call.requireAuth()
            val requests = FirestoreAdapter.query<Map<String, Any?>>(
                "blood_requests",
                listOf("status" to "pending"),
                limit = 50,
            )
            call.respondRaw(requests)
        }

        post("/cancel") {
            val auth = call.requireAuth()
            val req = call.receive<BloodRequestCancel>()

            val existing = FirestoreAdapter.get<Map<String, Any?>>("blood_requests", req.requestId)
                ?: throw IllegalStateException("Request not found")
            if (existing["patientUid"] != auth.uid) {
                throw IllegalStateException("Not your request")
            }
            if (existing["status"] != "pending") {
                throw IllegalStateException("Request is no longer pending")
            }

            FirestoreAdapter.setRaw(
                "blood_requests", req.requestId,
                mapOf(
                    "status" to "cancelled",
                    "cancelledAt" to System.currentTimeMillis(),
                ),
            )
            call.respond(success(OkResponse()))
        }

        post("/decline") {
            val auth = call.requireAuth()
            val req = call.receive<BloodRequestCancel>()

            val existing = FirestoreAdapter.get<Map<String, Any?>>("blood_requests", req.requestId)
                ?: throw IllegalStateException("Request not found")
            if (existing["status"] != "pending") {
                throw IllegalStateException("Request is no longer pending")
            }

            FirestoreAdapter.setRaw(
                "blood_requests", req.requestId,
                mapOf(
                    "status" to "declined",
                    "declinedBy" to auth.uid,
                    "declinedAt" to System.currentTimeMillis(),
                ),
            )
            call.respond(success(OkResponse()))
        }
    }

    route("/blood-donations") {
        post {
            val auth = call.requireAuth()
            val req = call.receive<DonorBookingCreate>()

            val user = FirestoreAdapter.getRaw("users", auth.uid)
            val donorName = (user?.get("name") as? String) ?: "Anonymous Donor"
            val donorPhone = (user?.get("phone") as? String) ?: ""

            val bookingId = FirestoreAdapter.newId()
            val booking = mapOf(
                "bookingId" to bookingId,
                "donorUid" to auth.uid,
                "donorName" to donorName,
                "donorPhone" to donorPhone,
                "bloodGroup" to req.bloodGroup,
                "scheduledDate" to req.scheduledDate,
                "scheduledTime" to req.scheduledTime,
                "date" to req.scheduledDate,
                "slotTime" to req.scheduledTime,
                "hospitalName" to req.hospitalName,
                "hospitalAddress" to req.hospitalAddress,
                "note" to req.note,
                "status" to "registered",
                "createdAt" to System.currentTimeMillis(),
            )
            FirestoreAdapter.setRaw("blood_donations", bookingId, booking)
            call.respondRaw(booking)
        }

        get("/mine") {
            val auth = call.requireAuth()
            val bookings = FirestoreAdapter.query<Map<String, Any?>>(
                "blood_donations",
                listOf("donorUid" to auth.uid),
                limit = 50,
            )
            call.respondRaw(bookings)
        }

        get("/upcoming") {
            val auth = call.requireAuth()
            val today = java.time.LocalDate.now().toString()
            val bookings = FirestoreAdapter.query<Map<String, Any?>>(
                "blood_donations",
                listOf("status" to "registered"),
                limit = 50,
            ).filter { (it["scheduledDate"] as? String ?: "") >= today }
            call.respondRaw(bookings)
        }
    }

    route("/blood-inventory") {
        get("/my") {
            val auth = call.requireAuth()
            val inv = FirestoreAdapter.get<Map<String, Any?>>("blood_inventory", auth.uid)
            call.respondRaw(inv ?: emptyMap<String, Any>())
        }

        put("/adjust") {
            val auth = call.requireAuth()
            val req = call.receive<BloodInventoryAdjustRequest>()

            FirestoreAdapter.runTransaction { txn ->
                val inv = txn.get("blood_inventory", auth.uid)
                val units = (inv?.data?.get("bloodGroupUnits") as? Map<*, *>) ?: emptyMap<String, Any>()
                val current = (units[req.bloodGroup] as? Number)?.toInt() ?: 0
                val newTotal = current + req.units
                if (newTotal < 0) throw IllegalStateException("Cannot go below zero")

                val updates = mutableMapOf<String, Any>(
                    "bloodGroupUnits.${req.bloodGroup}" to newTotal,
                    "lastUpdated" to System.currentTimeMillis(),
                )
                req.expiryDate?.let { updates["expiryDate"] = it }
                req.vaultLocation?.let { updates["vaultLocation"] = it }
                req.collectionDate?.let { updates["collectionDate"] = it }
                req.volumePerUnit?.let { updates["volumePerUnit"] = it }
                req.storageTemp?.let { updates["storageTemp"] = it }
                req.notes?.let { updates["notes"] = it }

                txn.update(
                    "blood_inventory", auth.uid,
                    updates,
                )
            }

            call.respond(success(OkResponse()))
        }
    }

    route("/donor-bookings") {
        get("/upcoming") {
            val auth = call.requireAuth()
            val today = java.time.LocalDate.now().toString()
            val bookings = FirestoreAdapter.query<Map<String, Any?>>(
                "blood_donations",
                listOf("status" to "registered"),
                limit = 100,
            ).filter { (it["scheduledDate"] as? String ?: "") >= today }
            call.respondRaw(bookings)
        }

        post("/cancel") {
            val auth = call.requireAuth()
            val req = call.receive<DonorBookingCancel>()

            FirestoreAdapter.setRaw(
                "blood_donations", req.bookingId,
                mapOf(
                    "status" to "cancelled",
                    "cancelledAt" to System.currentTimeMillis(),
                ),
            )
            call.respond(success(OkResponse()))
        }

        post("/check-in") {
            val auth = call.requireAuth()
            val req = call.receive<DonorBookingCancel>()

            FirestoreAdapter.setRaw(
                "blood_donations", req.bookingId,
                mapOf(
                    "status" to "checked_in",
                    "checkedInAt" to System.currentTimeMillis(),
                ),
            )
            call.respond(success(OkResponse()))
        }
    }
}
