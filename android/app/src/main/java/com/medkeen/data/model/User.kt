package com.medkeen.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: UserRole? = null,
    val isOnboarded: Boolean = false,
    val dpdpConsents: DpdpConsents = DpdpConsents(),
    val profile: UserProfile = UserProfile(),
    val fcmToken: String = ""
)

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val bloodGroup: String = "",
    val allergies: List<String> = emptyList(),
    val conditions: List<String> = emptyList(),
    val meds: List<String> = emptyList(),
    val emergencyContacts: List<EmergencyContact> = emptyList()
)

data class EmergencyContact(
    val id: String = "",
    val name: String = "",
    val relationship: String = "",
    val phone: String = "",
    val isPrimary: Boolean = false
)

data class DpdpConsents(
    val storeRecords: Boolean = false,
    val shareWithDoctor: Boolean = false,
    val aiProcessing: Boolean = false,
    val bloodNetwork: Boolean = false,
    val emergencyContact: Boolean = false,
    val dpdpVersion: String = "1.0"
)

enum class UserRole {
    PATIENT, DOCTOR, BLOOD_BANK, HOSPITAL, ADMIN
}
