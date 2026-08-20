package com.medvault.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.medvault.data.model.Doctor
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoctorRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun searchDoctors(specialty: String? = null): Result<List<Doctor>> {
        return try {
            var query: Query = firestore.collection("doctors")
                .whereEqualTo("verificationStatus", "VERIFIED")

            if (!specialty.isNullOrBlank()) {
                query = query.whereEqualTo("specialty", specialty)
            }

            val snapshot = query.get().await()
            val doctors = snapshot.documents.map { doc ->
                Doctor(
                    uid = doc.id,
                    displayName = doc.getString("displayName") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    specialty = doc.getString("specialty") ?: "",
                    licenseNumber = doc.getString("licenseNumber") ?: "",
                    verificationStatus = doc.getString("verificationStatus") ?: "UNVERIFIED",
                    rating = doc.getDouble("rating")?.toFloat() ?: 0f
                )
            }
            Result.success(doctors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoctor(uid: String): Result<Doctor> {
        return try {
            val doc = firestore.collection("doctors").document(uid).get().await()
            val doctor = Doctor(
                uid = doc.id,
                displayName = doc.getString("displayName") ?: "",
                photoUrl = doc.getString("photoUrl") ?: "",
                specialty = doc.getString("specialty") ?: "",
                licenseNumber = doc.getString("licenseNumber") ?: "",
                verificationStatus = doc.getString("verificationStatus") ?: "UNVERIFIED",
                rating = doc.getDouble("rating")?.toFloat() ?: 0f
            )
            Result.success(doctor)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
