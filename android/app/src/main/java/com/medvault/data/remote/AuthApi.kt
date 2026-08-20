package com.medvault.data.remote

import retrofit2.http.*

interface AuthApi {
    @POST("api/v1/auth/setup-role")
    suspend fun setupRole(@Body request: SetupRoleRequest): ApiResponse<SetupRoleResponse>

    @POST("api/v1/auth/setup-consents")
    suspend fun setupConsents(@Body request: SetupConsentsRequest): ApiResponse<Map<String, Any>>

    @GET("api/v1/auth/config")
    suspend fun getConfig(): ApiResponse<ConfigResponse>
}
