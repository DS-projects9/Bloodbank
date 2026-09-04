package com.medkeen.data.remote

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

    @POST("api/v1/auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): ApiResponse<LoginResponse>

    @GET("api/v1/auth/config")
    suspend fun getConfig(): ApiResponse<ConfigResponse>

    @POST("api/v1/auth/public-key")
    suspend fun uploadPublicKey(@Body request: SetPublicKeyRequest): ApiResponse<Map<String, Any>>

    @GET("api/v1/auth/public-key/{uid}")
    suspend fun getPublicKey(@Path("uid") uid: String): ApiResponse<Map<String, Any>>
}

data class GoogleAuthRequest(val idToken: String)
