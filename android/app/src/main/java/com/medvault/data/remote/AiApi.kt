package com.medvault.data.remote

import retrofit2.http.*

data class AiAnalyzeRequest(
    val sessionId: String? = null,
    val messages: List<AiMessage>
)

data class AiMessage(
    val role: String,
    val content: String
)

data class AiChatRequest(
    val sessionId: String,
    val message: String
)

data class AiResponse(
    val sessionId: String,
    val reply: String
)

interface AiApi {
    @POST("api/v1/ai/analyze")
    suspend fun analyze(@Body request: AiAnalyzeRequest): ApiResponse<AiResponse>

    @POST("api/v1/ai/chat")
    suspend fun chat(@Body request: AiChatRequest): ApiResponse<AiResponse>
}
