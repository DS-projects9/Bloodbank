package com.medkeen.routes

import com.medkeen.db.FirestoreAdapter
import com.medkeen.models.*
import com.medkeen.models.requests.*
import com.medkeen.plugins.requireAuth
import com.medkeen.utils.respondRaw
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

@Serializable
data class SetStatusRequest(
    val open: Boolean
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

            // Exclude doctors who toggled their clinic off for today.
            val closedUids = FirestoreAdapter.queryRaw("doctor_status", emptyList(), limit = 5000)
                .mapNotNull { snap ->
                    val d = snap.data as? Map<String, Any?>
                    if ((d?.get("open") as? Boolean) == false) snap.id else null
                }
                .toSet()

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
            call.respond(success(results.filter { it.uid !in closedUids }))
        }

        get("/{doctorUid}") {
            val auth = call.requireAuth()
            val doctorUid = call.parameters["doctorUid"]
                ?: throw IllegalArgumentException("Doctor UID required")
            val d = FirestoreAdapter.getRaw("users", doctorUid)
                ?: throw IllegalArgumentException("Doctor not found")
            val uid = (d["uid"] as? String)?.takeIf { it.isNotBlank() } ?: doctorUid
            val name = (d["name"] as? String)?.takeIf { it.isNotBlank() }
                ?: (d["displayName"] as? String) ?: "Unknown Doctor"
            call.respond(
                success(
                    mapOf(
                        "uid" to uid,
                        "name" to name,
                        "specialization" to (d["specialization"] as? String ?: ""),
                        "hospitalName" to (d["hospitalName"] as? String ?: ""),
                        "hospitalAddress" to (d["hospitalAddress"] as? String ?: ""),
                        "city" to (d["city"] as? String ?: ""),
                        "photoUrl" to (d["photoUrl"] as? String ?: ""),
                        "availableSlots" to 0,
                    )
                )
            )
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

    route("/me/status") {
        get {
            val auth = call.requireAuth()
            val doc = FirestoreAdapter.get<Map<String, Any?>>("doctor_status", auth.uid)
            val open = (doc?.get("open") as? Boolean) ?: true
            call.respond(success(mapOf("open" to open)))
        }

        put {
            val auth = call.requireAuth()
            val req = call.receive<SetStatusRequest>()
            FirestoreAdapter.setRaw("doctor_status", auth.uid, mapOf(
                "doctorUid" to auth.uid,
                "open" to req.open,
                "updatedAt" to System.currentTimeMillis(),
            ))
            call.respond(success(mapOf("open" to req.open)))
        }
    }

    post("/me/publish-next-week") {
        val auth = call.requireAuth()
        val req = call.receive<PublishNextWeekRequest>()

        val slotConfigs: List<Map<String, Any?>> = req.slots?.map { slot ->
            mapOf(
                "day" to slot.day,
                "startTime" to slot.startTime,
                "endTime" to slot.endTime,
                "slotMinutes" to slot.slotMinutes,
            )
        } ?: run {
            val config = FirestoreAdapter.get<Map<String, Any?>>("schedule_config", auth.uid)
                ?: throw IllegalStateException("No schedule configured")
            (config["slots"] as? List<*>)?.filterIsInstance<Map<String, Any?>>()
                ?: emptyList()
        }

        val weekStart = java.time.LocalDate.parse(req.weekStart)

        var batch = FirestoreAdapter.batch()
        var count = 0
        var ops = 0

        for (dayOffset in 0..6) {
            val date = weekStart.plusDays(dayOffset.toLong())
            val dayName = date.dayOfWeek.name

            val matchingSlots = slotConfigs.filter { (it as? Map<*, *>)?.get("day")?.toString()?.equals(dayName, ignoreCase = true) == true }

            for (slotConfig in matchingSlots) {
                val slotMap = slotConfig as? Map<*, *> ?: continue
                val startTime = slotMap["startTime"] as? String ?: continue
                val endTime = slotMap["endTime"] as? String ?: continue
                val slotMinutes = (slotMap["slotMinutes"] as? Number)?.toInt() ?: 30
                if (slotMinutes <= 0) continue

                val startMinutes = parseTimeToMinutes(startTime)
                val endMinutes = parseTimeToMinutes(endTime)

                var current = startMinutes
                while (current + slotMinutes <= endMinutes) {
                    val slotId = FirestoreAdapter.newId()
                    batch.set("slots", slotId, mapOf(
                        "slotId" to slotId,
                        "doctorUid" to auth.uid,
                        "date" to date.toString(),
                        "startTime" to minutesToTime(current),
                        "endTime" to minutesToTime(current + slotMinutes),
                        "status" to "available",
                        "createdAt" to System.currentTimeMillis(),
                    ))
                    current += slotMinutes
                    count++
                    ops++
                    if (ops >= 450) {
                        batch.commit()
                        batch = FirestoreAdapter.batch()
                        ops = 0
                    }
                }
            }
        }
        if (ops > 0) batch.commit()

        call.respond(success(SlotsCreatedResponse(slotsCreated = count)))
    }

    get("/me/slots") {
        val auth = call.requireAuth()
        val slots = FirestoreAdapter.query<Map<String, Any?>>(
            "slots",
            listOf("doctorUid" to auth.uid),
            limit = 1000
        )
        call.respondRaw(slots)
    }

    delete("/me/slots/{slotId}") {
        val auth = call.requireAuth()
        val slotId = call.parameters["slotId"] ?: throw IllegalArgumentException("Slot ID required")
        val slot = FirestoreAdapter.get<Map<String, Any?>>("slots", slotId)
            ?: throw IllegalArgumentException("Slot not found")
        if ((slot["doctorUid"] as? String) != auth.uid) {
            throw SecurityException("Not your slot")
        }
        val status = slot["status"] as? String
        if (status == "locked" || status == "booked") {
            throw IllegalStateException("Cannot remove a slot that is currently booked")
        }
        FirestoreAdapter.delete("slots", slotId)
        call.respond(success(OkResponse()))
    }
}

private fun parseTimeToMinutes(time: String): Int {
    val parts = time.trim().split(":")
    if (parts.size < 2) return 0
    val hour = parts[0].toIntOrNull() ?: 0
    val minutePart = parts[1]
    val isPm = minutePart.contains("PM", ignoreCase = true)
    val isAm = minutePart.contains("AM", ignoreCase = true)
    val minute = minutePart.replace(Regex("(?i)[^0-9]"), "").toIntOrNull() ?: 0
    val h = if (isPm || isAm) {
        val base = hour % 12
        if (isPm) base + 12 else base
    } else {
        hour
    }
    return h * 60 + minute
}

private fun minutesToTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}
