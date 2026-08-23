package com.medvault.data.remote

import retrofit2.http.*

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<LoginResponse>

    @POST("api/v1/auth/setup-role")
    suspend fun setupRole(@Body request: SetupRoleRequest): ApiResponse<SetupRoleResponse>

    @POST("api/v1/auth/setup-consents")
    suspend fun setupConsents(@Body request: SetupConsentsRequest): ApiResponse<ConsentsResponse>

    @GET("api/v1/auth/config")
    suspend fun getConfig(): ApiResponse<ConfigResponse>
}
