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
import kotlinx.serialization.Serializable

@Serializable
data class DoctorSearchResult(
    val uid: String,
    val name: String,
    val hospitalName: String?,
    val hospitalAddress: String?,
    val specialization: String?,
    val availableSlots: Int = 0,
)

fun Route.doctorRoutes() {
    route("/doctors") {
        get("/search") {
            val auth = call.requireAuth()
            val city = call.request.queryParameters["city"]
            val specialization = call.request.queryParameters["specialization"]

            val filters = mutableListOf<Pair<String, Any>>("role" to "DOCTOR")
            city?.let { filters.add("city" to it) }
            specialization?.let { filters.add("specialization" to it) }

            val docs = FirestoreAdapter.queryRaw("users", filters, limit = 50)

            // Count available slots in a forward-looking window so the patient
            // search reflects slots doctors have published (typically for the
            // upcoming week), not just the current calendar week.
            val today = java.time.LocalDate.now()
            val windowEnd = today.plusDays(14)
            val slotCounts = FirestoreAdapter.queryRaw("slots", listOf("status" to "available"), limit = 5000)
                .mapNotNull { it.data as? Map<String, Any?> }
                .filter {
                    val d = (it["date"] as? String) ?: ""
                    d >= today.toString() && d <= windowEnd.toString()
                }
                .groupingBy { (it["doctorUid"] as? String) ?: "" }
                .eachCount()

            val results = docs.map { snap ->
                val d = (snap.data as? Map<String, Any?>) ?: emptyMap<String, Any?>()
                val id = snap.id
                // Fall back to the document id / displayName for profiles created
                // through the app (which store `displayName`, not `name`/`uid`).
                val uid = (d["uid"] as? String)?.takeIf { it.isNotBlank() } ?: id
                DoctorSearchResult(
                    uid = uid,
                    name = (d["name"] as? String)?.takeIf { it.isNotBlank() }
                        ?: (d["displayName"] as? String) ?: "Unknown Doctor",
                    hospitalName = d["hospitalName"] as? String,
                    hospitalAddress = d["hospitalAddress"] as? String,
                    specialization = d["specialization"] as? String,
                    availableSlots = slotCounts[uid] ?: 0
                )
            }
            call.respond(success(results))
        }

        get("/{doctorUid}/slots") {
            val auth = call.requireAuth()
            val doctorUid = call.parameters["doctorUid"]
                ?: throw IllegalArgumentException("Doctor UID required")

            val today = java.time.LocalDate.now()
            val windowEnd = today.plusDays(14)

            val slots = FirestoreAdapter.query<Map<String, Any?>>(
                "slots",
                listOf("doctorUid" to doctorUid),
                limit = 200,
            ).filter { slot ->
                val date = slot["date"] as? String ?: ""
                date >= today.toString() && date <= windowEnd.toString()
            }.sortedBy { it["date"] as? String }

            call.respondRaw(slots)
        }

        // Doctor-facing patient access. Doctors may view a patient's full
        // profile and all of their vault records, and update profile fields.
        get("/patients/{patientUid}") {
            val auth = call.requireAuth()
            if (auth.role != "DOCTOR") throw SecurityException("Doctors only")

            val patientUid = call.parameters["patientUid"]
                ?: throw IllegalArgumentException("Patient UID required")

            val patient = FirestoreAdapter.get<Map<String, Any?>>("users", patientUid)
                ?: throw IllegalArgumentException("Patient not found")

            val documents = FirestoreAdapter.query<Map<String, Any?>>(
                "vault",
                listOf("ownerUid" to patientUid),
                limit = 200,
            )

            call.respond(success(mapOf("profile" to patient, "documents" to documents)))
        }

        patch("/patients/{patientUid}") {
            val auth = call.requireAuth()
            if (auth.role != "DOCTOR") throw SecurityException("Doctors only")

            val patientUid = call.parameters["patientUid"]
                ?: throw IllegalArgumentException("Patient UID required")

            val req = call.receive<UpdateProfileRequest>()

            val updates = mutableMapOf<String, Any?>()
            req.name?.let { updates["name"] = it }
            req.phone?.let { updates["phone"] = it }
            req.bloodGroup?.let { updates["bloodGroup"] = it }
            req.city?.let { updates["city"] = it }
            req.dob?.let { updates["dob"] = it }

            if (updates.isNotEmpty()) {
                updates["updatedAt"] = System.currentTimeMillis()
                FirestoreAdapter.setRaw("users", patientUid, updates)
            }

            call.respond(success(OkResponse()))
        }
    }

    route("/me/schedule") {
        get {
            val auth = call.requireAuth()
            val config = FirestoreAdapter.get<Map<String, Any?>>("schedule_config", auth.uid)
            call.respondRaw(config ?: emptyMap<String, Any>())
        }

        put {
            val auth = call.requireAuth()
            val req = call.receive<UpdateDoctorScheduleRequest>()

            val slotConfigs = req.slots.map { slot ->
                mapOf(
                    "day" to slot.day,
                    "startTime" to slot.startTime,
                    "endTime" to slot.endTime,
                    "slotMinutes" to slot.slotMinutes,
                )
            }

            FirestoreAdapter.setRaw("schedule_config", auth.uid, mapOf(
                "doctorUid" to auth.uid,
                "slots" to slotConfigs,
                "updatedAt" to System.currentTimeMillis(),
            ))

            call.respond(success(OkResponse()))
        }
    }

    post("/me/publish-next-week") {
        val auth = call.requireAuth()
        val req = call.receive<PublishNextWeekRequest>()

        val config = FirestoreAdapter.get<Map<String, Any?>>("schedule_config", auth.uid)
            ?: throw IllegalStateException("No schedule configured")

        val slots = config["slots"] as? List<*> ?: emptyList<Any?>()
        val weekStart = java.time.LocalDate.parse(req.weekStart)

        val batch = com.medvault.config.FirebaseProvider.firestore().batch()
        var count = 0

        for (dayOffset in 0..6) {
            val date = weekStart.plusDays(dayOffset.toLong())
            val dayName = date.dayOfWeek.name

            val matchingSlots = slots.filter { (it as? Map<*, *>)?.get("day") == dayName }

            for (slotConfig in matchingSlots) {
                val slotMap = slotConfig as? Map<*, *> ?: continue
                val startTime = slotMap["startTime"] as? String ?: continue
                val endTime = slotMap["endTime"] as? String ?: continue
                val slotMinutes = (slotMap["slotMinutes"] as? Number)?.toInt() ?: 30

                val startMinutes = parseTimeToMinutes(startTime)
                val endMinutes = parseTimeToMinutes(endTime)

                var current = startMinutes
                while (current + slotMinutes <= endMinutes) {
                    val slotRef = com.medvault.config.FirebaseProvider.firestore()
                        .collection("slots").document()
                    batch.set(slotRef, mapOf(
                        "slotId" to slotRef.id,
                        "doctorUid" to auth.uid,
                        "date" to date.toString(),
                        "startTime" to minutesToTime(current),
                        "endTime" to minutesToTime(current + slotMinutes),
                        "status" to "available",
                        "createdAt" to System.currentTimeMillis(),
                    ))
                    current += slotMinutes
                    count++
                }
            }
        }
        batch.commit().get()

        call.respond(success(SlotsCreatedResponse(slotsCreated = count)))
    }
}

private fun parseTimeToMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}

private fun minutesToTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}
