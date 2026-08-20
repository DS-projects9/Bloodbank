package com.medvault.data.model

data class BloodInventoryItem(
    val id: String = "",
    val bloodBankId: String = "",
    val bloodGroup: String = "",
    val unitsAvailable: Int = 0,
    val lastUpdatedAt: Long = 0
)

data class BloodRequest(
    val id: String = "",
    val requesterPatientId: String = "",
    val requesterPatientName: String = "",
    val requesterHospitalId: String? = null,
    val bloodGroup: String = "",
    val unitsRequired: Int = 0,
    val status: BloodRequestStatus = BloodRequestStatus.SEARCHING,
    val location: GeoLocation = GeoLocation(),
    val auditReveals: List<AuditReveal> = emptyList(),
    val createdAt: Long = 0
)

data class GeoLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class AuditReveal(
    val revealedAt: Long = 0,
    val revealedTo: String = "",
    val action: String = ""
)

enum class BloodRequestStatus {
    SEARCHING, NOTIFIED, FULFILLED, EXPIRED
}

data class Donor(
    val uid: String = "",
    val bloodGroup: String = "",
    val available: Boolean = false,
    val lastDonationAt: Long? = null
)
