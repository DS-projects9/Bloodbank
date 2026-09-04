package com.medkeen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medkeen.data.remote.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.HttpException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class AiChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionId: String? = null,
    val messages: List<AiChatMessage> = emptyList()
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun sendMessage(message: String) {
        if (_uiState.value.isLoading) return

        // First message of a (server-confirmed) session goes through /analyze;
        // follow-ups use /chat. Keying off sessionId rather than message count
        // so a failed first attempt retries cleanly instead of hitting /chat
        // with a session that doesn't exist yet.
        val isNewSession = _uiState.value.sessionId == null
        val sessionId = _uiState.value.sessionId
            ?: java.util.UUID.randomUUID().toString()

        val userMsg = AiChatMessage(role = "user", content = message)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg,
            isLoading = true,
            error = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = if (isNewSession) {
                    apiClient.aiApi.analyze(
                        AiAnalyzeRequest(
                            sessionId = sessionId,
                            messages = listOf(AiMessage(role = "user", content = message))
                        )
                    )
                } else {
                    apiClient.aiApi.chat(
                        AiChatRequest(sessionId = sessionId, message = message)
                    )
                }

                val reply = response.data?.reply ?: "No response"
                val assistantMsg = AiChatMessage(role = "assistant", content = reply)
                _uiState.value = _uiState.value.copy(
                    sessionId = response.data?.sessionId ?: sessionId,
                    messages = _uiState.value.messages + assistantMsg,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = friendlyAiError(e)
                )
            }
        }
    }

    private fun friendlyAiError(e: Throwable): String {
        val httpCode = (e as? HttpException)?.code()
        if (httpCode == 409 || httpCode == 429 || httpCode == 503) {
            return "AI service is unavailable right now (rate limited). Please try again in a moment."
        }
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("session not found") ->
                "Chat session expired. Please send your message again."
            msg.contains("llm provider error") || msg.contains("api key") ->
                "AI service is unavailable right now. Please try again later."
            msg.contains("network") || msg.contains("timeout") || msg.contains("connection") ||
                msg.contains("failed to connect") || msg.contains("unexpected end") ->
                "Network error. Please check your connection and try again."
            else -> e.message ?: "Something went wrong. Please try again."
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
