package com.medvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medvault.data.model.DpdpConsents
import com.medvault.data.model.User
import com.medvault.data.model.UserRole
import com.medvault.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
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
        viewModelScope.launch {
            val firebaseUser = authRepository.currentUser
            if (firebaseUser != null) {
                _authState.value = _authState.value.copy(isLoading = true)
                val result = authRepository.getCurrentUser()
                result.onSuccess { user ->
                    _authState.value = AuthState(user = user)
                }
                result.onFailure { e ->
                    _authState.value = AuthState(error = e.message)
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                _authState.value = AuthState(user = user)
            }
            result.onFailure { e ->
                _authState.value = AuthState(error = e.message)
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithEmail(email, password)
            result.onSuccess { user ->
                _authState.value = AuthState(user = user)
            }
            result.onFailure { e ->
                _authState.value = AuthState(error = e.message)
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
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = authRepository.signUpWithEmail(email, password, displayName, phone, bloodGroup, city)
            result.onSuccess { user ->
                _authState.value = AuthState(user = user)
            }
            result.onFailure { e ->
                _authState.value = AuthState(error = e.message)
            }
        }
    }

    fun selectRole(role: UserRole) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            val result = authRepository.updateRole(role)
            result.onSuccess {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    user = _authState.value.user?.copy(role = role)
                )
            }
            result.onFailure { e ->
                _authState.value = _authState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun acceptConsents(consents: DpdpConsents, role: UserRole) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            val roleResult = authRepository.updateRole(role)
            val consentResult = authRepository.updateConsents(consents)
            if (roleResult.isSuccess && consentResult.isSuccess) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    user = _authState.value.user?.copy(
                        role = role,
                        isOnboarded = true,
                        dpdpConsents = consents
                    )
                )
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = roleResult.exceptionOrNull()?.message ?: consentResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState()
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
}
