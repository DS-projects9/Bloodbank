package com.medkeen.data.model

data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val clinicId: String = "",
    val clinicName: String = "",
    val slot: AppointmentSlot = AppointmentSlot(),
    val status: AppointmentStatus = AppointmentStatus.PENDING_LOCK,
    val grants: List<DocumentGrant> = emptyList(),
    val createdAt: Long = 0
)

data class AppointmentSlot(
    val start: Long = 0,
    val end: Long = 0
)

data class DocumentGrant(
    val doctorId: String = "",
    val appointmentId: String = "",
    val validFrom: Long = 0,
    val validUntil: Long = 0,
    val status: GrantStatus = GrantStatus.ACTIVE,
    val documentIds: List<String> = emptyList()
)

enum class AppointmentStatus {
    PENDING_LOCK, BOOKED, IN_CONSULTATION, COMPLETED, CANCELLED, NO_SHOW
}

enum class GrantStatus {
    ACTIVE, REVOKED, EXPIRED
}
