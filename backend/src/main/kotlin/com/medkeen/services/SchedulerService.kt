package com.medkeen.services

import com.medkeen.db.FirestoreAdapter
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
        val cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15)

        val lockedSlots = FirestoreAdapter.queryRaw("slots", listOf("status" to "locked"), limit = 5000)
        var count = 0
        for (slot in lockedSlots) {
            val lockedAt = slot.getLong("lockedAt") ?: continue
            if (lockedAt < cutoff) {
                FirestoreAdapter.setRaw(
                    "slots", slot.id,
                    mapOf("status" to "available", "lockedBy" to null, "lockedAt" to null),
                )

                val slotId = slot.id
                val appts = FirestoreAdapter.queryRaw(
                    "appointments",
                    listOf("slotId" to slotId, "status" to "locked"),
                    limit = 100,
                )
                for (appt in appts) {
                    FirestoreAdapter.setRaw(
                        "appointments", appt.id,
                        mapOf("status" to "expired", "expiredAt" to System.currentTimeMillis()),
                    )
                }
                count++
            }
        }
        return count
    }

    private suspend fun expirePendingBloodRequests(): Int {
        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)

        val pending = FirestoreAdapter.queryRaw("blood_requests", listOf("status" to "pending"), limit = 2000)
        var count = 0
        for (req in pending) {
            val createdAt = req.getLong("createdAt") ?: continue
            if (createdAt < cutoff) {
                FirestoreAdapter.setRaw(
                    "blood_requests", req.id,
                    mapOf("status" to "expired", "expiredAt" to System.currentTimeMillis()),
                )
                count++
            }
        }
        return count
    }

    private suspend fun expireVaultDocuments(): Int {
        val now = System.currentTimeMillis()

        val active = FirestoreAdapter.queryRaw("vault", listOf("status" to "active"), limit = 2000)
        var count = 0
        for (doc in active) {
            val expiresAt = doc.getLong("expiresAt") ?: continue
            if (expiresAt < now) {
                FirestoreAdapter.setRaw("vault", doc.id, mapOf("status" to "expired"))
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
