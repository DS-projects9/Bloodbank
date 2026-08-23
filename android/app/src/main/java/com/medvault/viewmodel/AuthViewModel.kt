package com.medvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medvault.data.model.DpdpConsents
import com.medvault.data.model.User
import com.medvault.data.model.UserRole
import com.medvault.data.remote.TokenManager
import com.medvault.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isRestoringSession: Boolean = true,
    val user: User? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isLoggedIn: Boolean get() = authRepository.isLoggedIn
    val userRole: UserRole? get() = _authState.value.user?.role

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            if (TokenManager.isLoggedIn) {
                // Instant hydration from the persisted session so the correct
                // role dashboard renders without waiting on the network.
                TokenManager.loadUser()?.let { cached ->
                    _authState.value = AuthState(isRestoringSession = false, user = cached)
                }
                val result = authRepository.getCurrentUser()
                result.onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isRestoringSession = false,
                        user = user
                    )
                }
                result.onFailure { e ->
                    when {
                        e is com.medvault.data.repository.SessionExpiredException -> {
                            TokenManager.clear()
                            _authState.value = AuthState(isRestoringSession = false)
                        }
                        _authState.value.user == null -> {
                            // No cached identity and server unreachable.
                            TokenManager.clear()
                            _authState.value = AuthState(isRestoringSession = false)
                        }
                        else -> {
                            // Offline but cached session is valid; keep it.
                            _authState.value = _authState.value.copy(
                                isLoading = false,
                                isRestoringSession = false
                            )
                        }
                    }
                }
            } else {
                _authState.value = AuthState(isRestoringSession = false)
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithEmail(email, password)
            result.onSuccess { user ->
                _authState.value = AuthState(isRestoringSession = false, user = user)
            }
            result.onFailure { e ->
                _authState.value = AuthState(isRestoringSession = false, error = friendlyAuthError(e))
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        phone: String,
        bloodGroup: String,
        city: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = authRepository.signUpWithEmail(email, password, displayName, phone, bloodGroup, city)
            result.onSuccess { user ->
                _authState.value = AuthState(isRestoringSession = false, user = user)
            }
            result.onFailure { e ->
                _authState.value = AuthState(isRestoringSession = false, error = friendlyAuthError(e))
            }
        }
    }

    fun selectRole(role: UserRole) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(isLoading = true)
            val result = authRepository.updateRole(role)
            result.onSuccess {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    user = _authState.value.user?.copy(role = role)
                )
            }
            result.onFailure { e ->
                _authState.value = _authState.value.copy(isLoading = false, error = friendlyAuthError(e))
            }
        }
    }

    fun acceptConsents(consents: DpdpConsents, role: UserRole) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = _authState.value.copy(isLoading = true)
            val roleResult = authRepository.updateRole(role)
            val consentResult = authRepository.updateConsents(consents)
            if (roleResult.isSuccess && consentResult.isSuccess) {
                val updatedUser = _authState.value.user?.copy(
                    role = role,
                    isOnboarded = true,
                    dpdpConsents = consents
                )
                updatedUser?.let { TokenManager.saveUser(it) }
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    user = updatedUser
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = friendlyAuthError(
                        roleResult.exceptionOrNull() ?: consentResult.exceptionOrNull()
                        ?: Exception("Request failed")
                    )
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState(isRestoringSession = false)
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    private fun friendlyAuthError(e: Throwable): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("invalid credentials") || msg.contains("no account found") ->
                "Invalid email or password."
            msg.contains("already registered") || msg.contains("email already") ->
                "An account already exists with this email."
            msg.contains("network") || msg.contains("timeout") || msg.contains("connection") ->
                "Network error. Please check your connection."
            msg.contains("password") && msg.contains("6") ->
                "Password must be at least 6 characters."
            msg.contains("required") ->
                "Please fill in all required fields."
            else -> e.message ?: "Authentication failed. Please try again."
        }
    }
}
