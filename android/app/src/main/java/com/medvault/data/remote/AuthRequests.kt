package com.medvault.data.remote

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val role: String? = null,
)

data class LoginResponse(
    val token: String,
    val role: String?,
    val verified: Boolean,
    val name: String?,
    val email: String?,
    val uid: String? = null,
)

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

data class SetupConsentsRequest(
    val dataStorage: Boolean = false,
    val labResults: Boolean = false,
    val bloodDonation: Boolean = false,
)

data class SetupRoleResponse(
    val role: String,
    val verified: Boolean,
    val token: String? = null,
)

data class ConsentsResponse(
    val verified: Boolean,
    val token: String? = null,
)

data class ConfigResponse(
    val hasRole: Boolean,
    val hasConsents: Boolean,
    val role: String?,
    val name: String?,
    val email: String?,
    val uid: String? = null,
)
