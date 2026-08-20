package com.medvault.data.model

data class Doctor(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val specialty: String = "",
    val licenseNumber: String = "",
    val verificationStatus: String = "UNVERIFIED",
    val rating: Float = 0f,
    val clinics: List<Clinic> = emptyList(),
    val scheduleMatrix: List<ScheduleSlot> = emptyList(),
    val hospitalIds: List<String> = emptyList()
)

data class Clinic(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class ScheduleSlot(
    val id: String = "",
    val doctorId: String = "",
    val start: Long = 0,
    val end: Long = 0,
    val status: SlotStatus = SlotStatus.OPEN,
    val lockExpiresAt: Long? = null,
    val appointmentId: String? = null
)

enum class SlotStatus {
    OPEN, LOCKED, BOOKED, BLOCKED, UNAVAILABLE
}
