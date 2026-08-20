package com.medvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.medvault.data.remote.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun sendMessage(message: String) {
        val sessionId = _uiState.value.sessionId
            ?: java.util.UUID.randomUUID().toString()

        val userMsg = AiChatMessage(role = "user", content = message)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg,
            isLoading = true
        )

        viewModelScope.launch {
            try {
                val response = if (_uiState.value.messages.size <= 1) {
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
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
