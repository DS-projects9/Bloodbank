package com.medvault.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.medvault.data.model.Appointment
import com.medvault.data.model.AppointmentStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getAppointments(userId: String, role: String): Result<List<Appointment>> {
        return try {
            val field = if (role == "DOCTOR") "doctorId" else "patientId"
            val snapshot = firestore.collection("appointments")
                .whereEqualTo(field, userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val appointments = snapshot.documents.map { doc ->
                Appointment(
                    id = doc.id,
                    patientId = doc.getString("patientId") ?: "",
                    patientName = doc.getString("patientName") ?: "",
                    doctorId = doc.getString("doctorId") ?: "",
                    doctorName = doc.getString("doctorName") ?: "",
                    clinicId = doc.getString("clinicId") ?: "",
                    clinicName = doc.getString("clinicName") ?: "",
                    status = doc.getString("status")?.let {
                        try { AppointmentStatus.valueOf(it) } catch (e: Exception) { AppointmentStatus.BOOKED }
                    } ?: AppointmentStatus.BOOKED,
                    createdAt = doc.getLong("createdAt") ?: 0
                )
            }
            Result.success(appointments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAppointments(userId: String, role: String): Flow<List<Appointment>> = callbackFlow {
        val field = if (role == "DOCTOR") "doctorId" else "patientId"
        val listener = firestore.collection("appointments")
            .whereEqualTo(field, userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val appointments = snapshot?.documents?.map { doc ->
                    Appointment(
                        id = doc.id,
                        patientId = doc.getString("patientId") ?: "",
                        patientName = doc.getString("patientName") ?: "",
                        doctorId = doc.getString("doctorId") ?: "",
                        doctorName = doc.getString("doctorName") ?: "",
                        status = doc.getString("status")?.let {
                            try { AppointmentStatus.valueOf(it) } catch (e: Exception) { AppointmentStatus.BOOKED }
                        } ?: AppointmentStatus.BOOKED,
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                } ?: emptyList()
                trySend(appointments)
            }
        awaitClose { listener.remove() }
    }

    suspend fun cancelAppointment(appointmentId: String): Result<Unit> {
        return try {
            firestore.collection("appointments").document(appointmentId)
                .update("status", AppointmentStatus.CANCELLED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
