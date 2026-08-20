package com.medvault.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class AiAnalyzeRequest(
    val sessionId: String? = null,
    val messages: List<AiMessage>,
)

@Serializable
data class AiMessage(
    val role: String,
    val content: String,
)

@Serializable
data class AiChatRequest(
    val sessionId: String,
    val message: String,
)
