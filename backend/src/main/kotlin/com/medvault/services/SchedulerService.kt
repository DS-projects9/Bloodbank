package com.medvault.services

import com.google.cloud.firestore.Firestore
import com.medvault.config.FirebaseProvider
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

object SchedulerService {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    runAllJobs()
                } catch (e: Exception) {
                    println("[Scheduler] Error: ${e.message}")
                }
                delay(TimeUnit.MINUTES.toMillis(1))
            }
        }
        println("[Scheduler] Started — running every 60s")
    }

    fun stop() {
        job?.cancel()
        println("[Scheduler] Stopped")
    }

    suspend fun runAllJobs(): SchedulerResult {
        val slotExpiry = expireLockedSlots()
        val bloodExpiry = expirePendingBloodRequests()
        val vaultExpiry = expireVaultDocuments()

        val result = SchedulerResult(
            lockedSlotsExpired = slotExpiry,
            bloodRequestsExpired = bloodExpiry,
            vaultDocsExpired = vaultExpiry,
            ranAt = System.currentTimeMillis(),
        )
        println("[Scheduler] Ran: ${result.lockedSlotsExpired} slots, " +
                "${result.bloodRequestsExpired} blood requests, " +
                "${result.vaultDocsExpired} vault docs expired")
        return result
    }

    private suspend fun expireLockedSlots(): Int {
        val db = FirebaseProvider.firestore()
        val cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15)

        val lockedSlots = db.collection("slots")
            .whereEqualTo("status", "locked")
            .get().get().documents

        var count = 0
        for (slot in lockedSlots) {
            val lockedAt = (slot.get("lockedAt") as? Number)?.toLong() ?: continue
            if (lockedAt < cutoff) {
                slot.reference.update(mapOf(
                    "status" to "available",
                    "lockedBy" to null,
                    "lockedAt" to null,
                )).get()

                val slotId = slot.id
                val appts = db.collection("appointments")
                    .whereEqualTo("slotId", slotId)
                    .whereEqualTo("status", "locked")
                    .get().get().documents

                for (appt in appts) {
                    appt.reference.update(mapOf(
                        "status" to "expired",
                        "expiredAt" to System.currentTimeMillis(),
                    )).get()
                }
                count++
            }
        }
        return count
    }

    private suspend fun expirePendingBloodRequests(): Int {
        val db = FirebaseProvider.firestore()
        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)

        val pending = db.collection("blood_requests")
            .whereEqualTo("status", "pending")
            .get().get().documents

        var count = 0
        for (req in pending) {
            val createdAt = (req.get("createdAt") as? Number)?.toLong() ?: continue
            if (createdAt < cutoff) {
                req.reference.update(mapOf(
                    "status" to "expired",
                    "expiredAt" to System.currentTimeMillis(),
                )).get()
                count++
            }
        }
        return count
    }

    private suspend fun expireVaultDocuments(): Int {
        val db = FirebaseProvider.firestore()
        val now = System.currentTimeMillis()

        val active = db.collection("vault")
            .whereEqualTo("status", "active")
            .get().get().documents

        var count = 0
        for (doc in active) {
            val expiresAt = (doc.get("expiresAt") as? Number)?.toLong() ?: continue
            if (expiresAt < now) {
                doc.reference.update(mapOf(
                    "status" to "expired",
                )).get()
                count++
            }
        }
        return count
    }
}

@Serializable
data class SchedulerResult(
    val lockedSlotsExpired: Int,
    val bloodRequestsExpired: Int,
    val vaultDocsExpired: Int,
    val ranAt: Long,
)
