package com.medvault.routes

import com.medvault.LlmException
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun Route.aiRoutes() {
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
        }
    }

    // Any OpenAI-compatible provider works (OpenAI, OpenCode Zen, LM Studio...):
    // POST {baseUrl}/chat/completions with Bearer key and {model, messages}.
    suspend fun ApplicationCall.completeChat(systemPrompt: String, chatMessages: List<Map<String, String>>): String {
        val config = AppConfig.load()
        val apiKey = config.openaiApiKey
            ?: throw LlmException("LLM API key not configured (OPENCODE_API_KEY / OPENAI_API_KEY)")

        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to systemPrompt))
        messages.addAll(chatMessages)

        var lastDetail: String? = null
        repeat(3) { attempt ->
            try {
                val response = httpClient.post("${config.llmBaseUrl}/chat/completions") {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProviderChatRequest(
                            model = config.llmModel,
                            messages = messages.map { ProviderMessage(it["role"] ?: "", it["content"] ?: "") },
                        )
                    )
                }

                val body = runCatching { response.body<JsonObject>() }.getOrNull()
                // Some providers return 200 with an {"error": ...} payload; treat that as failure.
                val providerError = (body?.get("error") as? JsonObject)?.get("message")
                    ?.let { (it as? JsonPrimitive)?.content }
                    ?: (body?.get("error") as? JsonPrimitive)?.content

                if (response.status.isSuccess() && providerError == null) {
                    val choices = body?.get("choices") as? kotlinx.serialization.json.JsonArray
                    val messageObj = choices?.firstOrNull()?.let { (it as? JsonObject)?.get("message") as? JsonObject }
                    val contentPrimitive = messageObj?.get("content") as? JsonPrimitive
                    return contentPrimitive?.content ?: "No response"
                }

                lastDetail = providerError ?: "HTTP ${response.status.value}"

                // Retry (with backoff) on rate limiting; fail fast on other errors.
                if (response.status.value == 429 || providerError?.contains("rate limit", ignoreCase = true) == true) {
                    kotlinx.coroutines.delay((attempt + 1) * 1500L)
                    return@repeat
                }
                throw LlmException("LLM provider error: $lastDetail")
            } catch (e: LlmException) {
                throw e
            } catch (e: Exception) {
                lastDetail = e.message ?: "network error"
                if (attempt < 2) {
                    kotlinx.coroutines.delay((attempt + 1) * 1000L)
                    return@repeat
                }
                throw LlmException("LLM provider error: $lastDetail")
            }
        }
        throw LlmException("LLM provider error: ${lastDetail ?: "unknown"}")
    }

    val medvaultSystemPrompt = """
        You are the MedVault in-app health assistant. MedVault is a medical records app with three roles.

        Patient features:
        - Find Doctor: search doctors by specialty, pick a date and time slot, tap "Book Appointment". The booking appears under Appointments; doctors confirm from their queue.
        - Emergency Blood Search: choose blood group, units, timing and purpose, then "Find Matching Blood Banks" to see nearby stock; broadcast or send a direct request to a bank.
        - Schedule Blood Donation: register as a donor for a date/time at a hospital.
        - Consent Manager: control data-storage, lab-result sharing and blood-network consents.
        - Health Vault: documents are time-boxed — doctors get access only for a limited window (7 days) after sharing; access auto-expires.
        - Profile: emergency contacts, biometric lock, logout.

        Doctor features: appointment queue, confirming appointments with diagnosis/follow-up, viewing shared patient records.
        Blood-bank features: inventory levels per blood group, fulfilling blood requests, upcoming donor bookings.

        Your job: answer questions about the user's health in general terms, explain reports they describe, and guide them step-by-step on how to use the features above. For anything urgent (chest pain, severe bleeding, unconsciousness), tell them to use Emergency Blood Search or contact emergency services immediately. Never invent personal data you cannot see; if asked about their specific records, explain where in the app to find them. Always recommend consulting a doctor for medical decisions.
    """.trim()

    route("/ai") {
        post("/analyze") {
            val auth = call.requireAuth()
            val req = call.receive<AiAnalyzeRequest>()
            val sessionId = req.sessionId ?: java.util.UUID.randomUUID().toString()

            val replyContent = call.completeChat(medvaultSystemPrompt, req.messages.map {
                mapOf("role" to it.role, "content" to it.content)
            })

            val session = FirestoreAdapter.get<Map<String, Any?>>("ai_sessions", sessionId)
            val existingMessages = extractMessages(session)

            val assistantMsg = mapOf("role" to "assistant", "content" to replyContent)
            val updatedMessages = existingMessages + req.messages.map {
                mapOf("role" to it.role, "content" to it.content)
            } + assistantMsg

            val createdAt = session?.get("createdAt") ?: System.currentTimeMillis()
            FirestoreAdapter.setRaw("ai_sessions", sessionId, mapOf(
                "sessionId" to sessionId,
                "uid" to auth.uid,
                "messages" to updatedMessages,
                "createdAt" to createdAt,
                "updatedAt" to System.currentTimeMillis(),
            ))

            call.respond(success(mapOf("sessionId" to sessionId, "reply" to replyContent)))
        }

        post("/chat") {
            val auth = call.requireAuth()
            val req = call.receive<AiChatRequest>()

            val session = FirestoreAdapter.get<Map<String, Any?>>("ai_sessions", req.sessionId)
                ?: throw IllegalArgumentException("Session not found")
            if ((session["uid"] as? String) != auth.uid) {
                throw SecurityException("Not your session")
            }

            val existingMessages = extractMessages(session)
            val history = existingMessages + mapOf("role" to "user", "content" to req.message)
            val replyContent = call.completeChat(medvaultSystemPrompt, history.takeLast(20))

            val assistantMsg = mapOf("role" to "assistant", "content" to replyContent)
            FirestoreAdapter.setRaw("ai_sessions", req.sessionId, mapOf(
                "messages" to history + assistantMsg,
                "updatedAt" to System.currentTimeMillis(),
            ))

            call.respond(success(mapOf("sessionId" to req.sessionId, "reply" to replyContent)))
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

@Serializable
private data class ProviderMessage(val role: String, val content: String)

@Serializable
private data class ProviderChatRequest(
    val model: String,
    val messages: List<ProviderMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 1000,
    val temperature: Double = 0.4,
)
