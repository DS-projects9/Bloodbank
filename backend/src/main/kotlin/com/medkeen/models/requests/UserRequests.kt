package com.medkeen.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class SetupRoleRequest(
    val role: String,
    val name: String,
    val phone: String,
    val dob: String? = null,
    val bloodGroup: String? = null,
    val city: String? = null,
    val hospitalName: String? = null,
    val hospitalAddress: String? = null,
    val specialization: String? = null,
    val licenseNumber: String? = null,
    val bankName: String? = null,
    val bankAddress: String? = null,
    val bloodBankLicense: String? = null,
)

@Serializable
data class SetupConsentsRequest(
    val dataStorage: Boolean = false,
    val labResults: Boolean = false,
    val bloodDonation: Boolean = false,
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    val bloodGroup: String? = null,
    val city: String? = null,
    val dob: String? = null,
    val hospitalName: String? = null,
    val hospitalAddress: String? = null,
    val specialization: String? = null,
    val licenseNumber: String? = null,
    val bankName: String? = null,
    val bankAddress: String? = null,
    val bloodBankLicense: String? = null,
)

@Serializable
data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val relationship: String = "",
)

@Serializable
data class UpdateContactsRequest(
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val primaryContactUid: String? = null,
)

@Serializable
data class UpdateFcmTokenRequest(
    val fcmToken: String,
)

@Serializable
data class UpdateConsentsRequest(
    val dataStorage: Boolean = false,
    val labResults: Boolean = false,
    val bloodDonation: Boolean = false,
)
