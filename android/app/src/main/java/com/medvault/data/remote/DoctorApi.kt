package com.medvault.data.remote

import retrofit2.http.*

data class DoctorSearchResult(
    val uid: String,
    val name: String,
    val hospitalName: String?,
    val hospitalAddress: String?,
    val specialization: String?,
    val availableSlots: Int = 0
)

data class SlotUpdate(
    val day: String,
    val startTime: String,
    val endTime: String,
    val slotMinutes: Int
)

data class UpdateDoctorScheduleRequest(
    val slots: List<SlotUpdate>
)

data class PublishNextWeekRequest(
    val weekStart: String
)

interface DoctorApi {
    @GET("api/v1/doctors/search")
    suspend fun searchDoctors(
        @Query("city") city: String? = null,
        @Query("specialization") specialization: String? = null
    ): ApiResponse<List<DoctorSearchResult>>

    @GET("api/v1/doctors/{doctorUid}/slots")
    suspend fun getDoctorSlots(@Path("doctorUid") doctorUid: String): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/me/schedule")
    suspend fun getMySchedule(): ApiResponse<Map<String, Any>>

    @PUT("api/v1/me/schedule")
    suspend fun updateMySchedule(@Body request: UpdateDoctorScheduleRequest): ApiResponse<Map<String, Any>>

    @POST("api/v1/doctors/me/publish-next-week")
    suspend fun publishNextWeek(@Body request: PublishNextWeekRequest): ApiResponse<Map<String, Any>>

    @GET("api/v1/doctors/patients/{patientUid}")
    suspend fun getPatient(@Path("patientUid") patientUid: String): ApiResponse<Map<String, Any>>

    @PATCH("api/v1/doctors/patients/{patientUid}")
    suspend fun updatePatient(
        @Path("patientUid") patientUid: String,
        @Body request: UpdateProfileRequest
    ): ApiResponse<Map<String, Any>>
}
