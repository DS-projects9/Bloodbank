package com.medvault.data.remote

import retrofit2.http.*

data class BloodRequestCreate(
    val patientName: String,
    val bloodGroup: String,
    val units: Int,
    val hospitalName: String,
    val hospitalAddress: String,
    val urgency: String = "normal",
    val note: String? = null
)

data class BloodFulfillRequest(
    val requestId: String,
    val units: Int
)

data class DonorBookingCreate(
    val bloodGroup: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val hospitalName: String,
    val hospitalAddress: String,
    val note: String? = null
)

data class BloodInventoryAdjustRequest(
    val bloodGroup: String,
    val units: Int,
    val reason: String
)

interface BloodApi {
    @POST("api/v1/blood-requests")
    suspend fun createBloodRequest(@Body request: BloodRequestCreate): ApiResponse<Map<String, Any>>

    @GET("api/v1/blood-requests/mine")
    suspend fun getMyBloodRequests(): ApiResponse<List<Map<String, Any>>>

    @POST("api/v1/blood-requests/fulfill")
    suspend fun fulfillBloodRequest(@Body request: BloodFulfillRequest): ApiResponse<Map<String, Any>>

    @GET("api/v1/blood-requests/nearby")
    suspend fun getNearbyRequests(): ApiResponse<List<Map<String, Any>>>

    @POST("api/v1/blood-donations")
    suspend fun createDonorBooking(@Body request: DonorBookingCreate): ApiResponse<Map<String, Any>>

    @GET("api/v1/blood-donations/mine")
    suspend fun getMyDonations(): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/blood-donations/upcoming")
    suspend fun getUpcomingDonations(): ApiResponse<List<Map<String, Any>>>

    @GET("api/v1/blood-inventory/my")
    suspend fun getMyInventory(): ApiResponse<Map<String, Any>>

    @GET("api/v1/blood-banks/search")
    suspend fun searchBloodBanks(@Query("bloodGroup") bloodGroup: String? = null): ApiResponse<List<Map<String, Any>>>

    @PUT("api/v1/blood-inventory/adjust")
    suspend fun adjustInventory(@Body request: BloodInventoryAdjustRequest): ApiResponse<Map<String, Any>>

    @GET("api/v1/donor-bookings/upcoming")
    suspend fun getUpcomingDonorBookings(): ApiResponse<List<Map<String, Any>>>

    @POST("api/v1/donor-bookings/cancel")
    suspend fun cancelDonorBooking(@Body request: Map<String, String>): ApiResponse<Map<String, Any>>
}
