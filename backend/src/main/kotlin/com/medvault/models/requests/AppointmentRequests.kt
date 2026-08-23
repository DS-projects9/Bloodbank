package com.medvault.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class LockSlotRequest(
    val slotId: String,
    val doctorUid: String,
    val patientNote: String? = null,
)

@Serializable
data class ConfirmAppointmentRequest(
    val appointmentId: String,
    val diagnosis: String? = null,
    val followUpDate: String? = null,
    val filesToShare: List<String> = emptyList(),
)

@Serializable
data class CancelAppointmentRequest(
    val appointmentId: String,
    val reason: String,
)

@Serializable
data class ShareFilesRequest(
    val fileNames: List<String> = emptyList(),
    val durationMinutes: Long? = null,
)

data class ExtendAccessRequest(
    val durationMinutes: Long,
)
