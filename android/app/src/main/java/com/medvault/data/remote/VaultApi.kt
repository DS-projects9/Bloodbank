package com.medvault.data.remote

import retrofit2.http.*

data class VaultDocument(
    val documentId: String,
    val ownerUid: String,
    val sharedWith: List<String> = emptyList(),
    val fileNames: List<String> = emptyList(),
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

    @GET("api/v1/vault/download/{documentId}")
    suspend fun getDownloadUrls(@Path("documentId") documentId: String): ApiResponse<Map<String, String>>
}
