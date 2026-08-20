package com.medvault.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.medvault.data.model.BloodInventoryItem
import com.medvault.data.model.BloodRequest
import com.medvault.data.model.BloodRequestStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloodBankRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getBloodInventory(bloodBankId: String): Result<List<BloodInventoryItem>> {
        return try {
            val snapshot = firestore.collection("blood_inventory")
                .whereEqualTo("bloodBankId", bloodBankId)
                .get()
                .await()

            val items = snapshot.documents.map { doc ->
                BloodInventoryItem(
                    id = doc.id,
                    bloodBankId = doc.getString("bloodBankId") ?: "",
                    bloodGroup = doc.getString("bloodGroup") ?: "",
                    unitsAvailable = doc.getLong("unitsAvailable")?.toInt() ?: 0,
                    lastUpdatedAt = doc.getLong("lastUpdatedAt") ?: 0
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBloodUnits(itemId: String, delta: Int): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection("blood_inventory").document(itemId)
                val doc = transaction.get(docRef)
                val currentUnits = doc.getLong("unitsAvailable")?.toInt() ?: 0
                val newUnits = maxOf(0, currentUnits + delta)
                transaction.update(docRef, mapOf(
                    "unitsAvailable" to newUnits,
                    "lastUpdatedAt" to System.currentTimeMillis()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeBloodRequests(bloodBankId: String): Flow<List<BloodRequest>> = callbackFlow {
        val listener = firestore.collection("blood_requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.map { doc ->
                    BloodRequest(
                        id = doc.id,
                        requesterPatientId = doc.getString("requesterPatientId") ?: "",
                        requesterPatientName = doc.getString("requesterPatientName") ?: "",
                        bloodGroup = doc.getString("bloodGroup") ?: "",
                        unitsRequired = doc.getLong("unitsRequired")?.toInt() ?: 0,
                        status = doc.getString("status")?.let {
                            try { BloodRequestStatus.valueOf(it) } catch (e: Exception) { BloodRequestStatus.SEARCHING }
                        } ?: BloodRequestStatus.SEARCHING,
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    suspend fun fulfillBloodRequest(requestId: String, bloodBankId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val requestRef = firestore.collection("blood_requests").document(requestId)
                transaction.update(requestRef, "status", BloodRequestStatus.FULFILLED.name)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
