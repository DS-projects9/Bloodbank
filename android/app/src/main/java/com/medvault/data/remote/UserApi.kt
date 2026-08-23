package com.medvault.data.remote

import retrofit2.http.*

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
    val bloodBankLicense: String? = null
)

data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val relationship: String = ""
)

data class UpdateContactsRequest(
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val primaryContactUid: String? = null
)

data class UpdateFcmTokenRequest(
    val fcmToken: String
)

data class UpdateConsentsRequest(
    val dataStorage: Boolean = false,
    val labResults: Boolean = false,
    val bloodDonation: Boolean = false,
)

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getMe(): ApiResponse<Map<String, Any>>

    @PATCH("api/v1/users/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): ApiResponse<Map<String, Any>>

    @PUT("api/v1/users/contacts")
    suspend fun updateContacts(@Body request: UpdateContactsRequest): ApiResponse<Map<String, Any>>

    @POST("api/v1/users/fcm-token")
    suspend fun updateFcmToken(@Body request: UpdateFcmTokenRequest): ApiResponse<Map<String, Any>>

    @PATCH("api/v1/users/consents")
    suspend fun updateConsents(@Body request: UpdateConsentsRequest): ApiResponse<Map<String, Any>>

    @DELETE("api/v1/users/me/reports")
    suspend fun deleteReports(): ApiResponse<Map<String, Any>>

    @DELETE("api/v1/users/me")
    suspend fun deleteAccount(): ApiResponse<Map<String, Any>>
}
