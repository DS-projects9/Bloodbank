package com.medvault.data.remote

import retrofit2.http.*

data class VaultDocument(
    val documentId: String,
    val ownerUid: String,
    val sharedWith: List<String> = emptyList(),
    val fileNames: List<String> = emptyList(),
    val appointmentId: String? = null,
    val status: String = "active",
    val durationMinutes: Long = 0,
    val viewedAt: Long = 0,
    val createdAt: Long = 0,
    val expiresAt: Long = 0
)

data class SignedUrlResponse(
    val url: String,
    val expiresInMinutes: Int
)

interface VaultApi {
    @GET("api/v1/vault/documents")
    suspend fun getDocuments(): ApiResponse<List<VaultDocument>>

    @POST("api/v1/vault/upload-init")
    suspend fun initUpload(@Body request: UploadInitRequest): ApiResponse<Map<String, Any>>

    @GET("api/v1/vault/download/{documentId}")
    suspend fun getDownloadUrls(@Path("documentId") documentId: String): ApiResponse<Map<String, String>>

    @POST("api/v1/vault/{documentId}/open")
    suspend fun openDocument(@Path("documentId") documentId: String): ApiResponse<Map<String, Any>>
}

data class UploadInitRequest(
    val fileName: String,
    val contentType: String = "application/octet-stream",
)
