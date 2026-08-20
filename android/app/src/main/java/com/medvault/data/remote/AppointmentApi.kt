package com.medvault.data.remote

import retrofit2.http.*

data class LockSlotRequest(
    val slotId: String,
    val doctorUid: String,
    val patientNote: String? = null
)

data class ConfirmAppointmentRequest(
    val appointmentId: String,
    val diagnosis: String? = null,
    val followUpDate: String? = null,
    val filesToShare: List<String> = emptyList()
)

data class CancelAppointmentRequest(
    val appointmentId: String,
    val reason: String
)

interface AppointmentApi {
    @POST("api/v1/appointments/lock")
    suspend fun lockSlot(@Body request: LockSlotRequest): ApiResponse<Map<String, Any>>

    @POST("api/v1/appointments/confirm")
    suspend fun confirmAppointment(@Body request: ConfirmAppointmentRequest): ApiResponse<Map<String, Any>>

    @POST("api/v1/appointments/cancel")
    suspend fun cancelAppointment(@Body request: CancelAppointmentRequest): ApiResponse<Map<String, Any>>

    @GET("api/v1/appointments/mine")
    suspend fun getMyAppointments(): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/appointments/{appointmentId}")
    suspend fun getAppointment(@Path("appointmentId") appointmentId: String): ApiResponse<Map<String, Any>>
}
