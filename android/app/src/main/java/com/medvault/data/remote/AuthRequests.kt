package com.medvault.data.remote

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
    val bloodBankLicense: String? = null
)

data class SetupConsentsRequest(
    val dataStorage: Boolean = false,
    val labResults: Boolean = false,
    val bloodDonation: Boolean = false
)

data class SetupRoleResponse(
    val role: String,
    val verified: Boolean
)

data class ConfigResponse(
    val hasRole: Boolean,
    val hasConsents: Boolean,
    val role: String?,
    val name: String?,
    val email: String?
)
