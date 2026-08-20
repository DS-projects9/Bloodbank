package com.medvault.routes

import com.medvault.config.AppConfig
import com.medvault.db.FirestoreAdapter
import com.medvault.models.*
import com.medvault.models.requests.*
import com.medvault.plugins.requireAuth
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.header
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun Route.aiRoutes() {
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    route("/ai") {
        post("/analyze") {
            val auth = call.requireAuth()
            val config = AppConfig.load()
            val apiKey = config.openaiApiKey
                ?: throw IllegalStateException("OPENAI_API_KEY not configured")

            val req = call.receive<AiAnalyzeRequest>()
            val sessionId = req.sessionId ?: java.util.UUID.randomUUID().toString()

            val messages = mutableListOf<Map<String, String>>()
            messages.add(mapOf("role" to "system", "content" to "You are a medical AI assistant for MedVault. Be helpful, accurate, and always recommend consulting a doctor for medical decisions."))
            messages.addAll(req.messages.map { mapOf("role" to it.role, "content" to it.content) })

            val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "model" to "gpt-4o-mini",
                    "messages" to messages,
                    "max_tokens" to 1000,
                ))
            }

            val body = response.body<JsonObject>()
            val choices = body["choices"] as? kotlinx.serialization.json.JsonArray
            val messageObj = choices?.firstOrNull()?.let { (it as? JsonObject)?.get("message") as? JsonObject }
            val contentPrimitive = messageObj?.get("content") as? JsonPrimitive
            val replyContent = contentPrimitive?.content ?: "No response"

            val session = FirestoreAdapter.get<Map<String, Any?>>("ai_sessions", sessionId)
            val existingMessages = extractMessages(session)

            val assistantMsg = mapOf("role" to "assistant", "content" to replyContent)
            val updatedMessages = existingMessages + messages.drop(1) + assistantMsg

            val createdAt = session?.get("createdAt") ?: System.currentTimeMillis()
            FirestoreAdapter.setRaw("ai_sessions", sessionId, mapOf(
                "sessionId" to sessionId,
                "uid" to auth.uid,
                "messages" to updatedMessages,
                "createdAt" to createdAt,
                "updatedAt" to System.currentTimeMillis(),
            ))

            val result = mapOf("sessionId" to sessionId, "reply" to replyContent)
            call.respond(success(result))
        }

        post("/chat") {
            val auth = call.requireAuth()
            val config = AppConfig.load()
            val apiKey = config.openaiApiKey
                ?: throw IllegalStateException("OPENAI_API_KEY not configured")

            val req = call.receive<AiChatRequest>()

            val session = FirestoreAdapter.get<Map<String, Any?>>("ai_sessions", req.sessionId)
                ?: throw IllegalArgumentException("Session not found")

            val existingMessages = extractMessages(session)

            val messages = mutableListOf<Map<String, String>>()
            messages.add(mapOf("role" to "system", "content" to "You are a medical AI assistant for MedVault. Be helpful, accurate, and always recommend consulting a doctor for medical decisions."))
            messages.addAll(existingMessages)
            messages.add(mapOf("role" to "user", "content" to req.message))

            val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "model" to "gpt-4o-mini",
                    "messages" to messages,
                    "max_tokens" to 1000,
                ))
            }

            val body = response.body<JsonObject>()
            val choices = body["choices"] as? kotlinx.serialization.json.JsonArray
            val messageObj = choices?.firstOrNull()?.let { (it as? JsonObject)?.get("message") as? JsonObject }
            val contentPrimitive = messageObj?.get("content") as? JsonPrimitive
            val replyContent = contentPrimitive?.content ?: "No response"

            val userMsg = mapOf("role" to "user", "content" to req.message)
            val assistantMsg = mapOf("role" to "assistant", "content" to replyContent)
            val updatedMessages = existingMessages + userMsg + assistantMsg

            FirestoreAdapter.setRaw("ai_sessions", req.sessionId, mapOf(
                "messages" to updatedMessages,
                "updatedAt" to System.currentTimeMillis(),
            ))

            val result = mapOf("sessionId" to req.sessionId, "reply" to replyContent)
            call.respond(success(result))
        }
    }
}

private fun extractMessages(session: Map<String, Any?>?): List<Map<String, String>> {
    val raw = session?.get("messages") as? List<*> ?: return emptyList()
    return raw.mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        val role = map["role"] as? String ?: ""
        val content = map["content"] as? String ?: ""
        mapOf("role" to role, "content" to content)
    }
}
