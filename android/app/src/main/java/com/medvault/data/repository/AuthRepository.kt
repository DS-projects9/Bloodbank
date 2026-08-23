package com.medvault.data.repository

import com.medvault.data.model.DpdpConsents
import com.medvault.data.model.User
import com.medvault.data.model.UserRole
import com.medvault.data.remote.ApiClient
import com.medvault.data.remote.TokenManager
import com.medvault.data.remote.LoginRequest
import com.medvault.data.remote.RegisterRequest
import com.medvault.data.remote.SetupRoleRequest
import com.medvault.data.remote.SetupConsentsRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiClient: ApiClient
) {
    val isLoggedIn get() = TokenManager.isLoggedIn

    fun signOut() {
        TokenManager.clear()
    }

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val response = apiClient.authApi.login(LoginRequest(email, password))
            if (response.ok && response.data != null) {
                TokenManager.setToken(response.data.token)
                val user = User(
                    uid = response.data.uid ?: "",
                    email = response.data.email ?: email,
                    displayName = response.data.name ?: "",
                    role = response.data.role?.let {
                        try { UserRole.valueOf(it) } catch (_: Exception) { null }
                    },
                    isOnboarded = response.data.role != null,
                )
                TokenManager.saveUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception(response.error ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        phone: String,
        bloodGroup: String,
        city: String,
    ): Result<User> {
        return try {
            val response = apiClient.authApi.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    name = displayName,
                    phone = phone,
                    city = city,
                )
            )
            if (response.ok && response.data != null) {
                TokenManager.setToken(response.data.token)
                val user = User(
                    uid = "",
                    email = response.data.email ?: email,
                    displayName = response.data.name ?: displayName,
                    role = null,
                    isOnboarded = false,
                )
                TokenManager.saveUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception(response.error ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRole(role: UserRole, name: String = "", phone: String = ""): Result<String?> {
        return try {
            val response = apiClient.authApi.setupRole(
                SetupRoleRequest(role = role.name, name = name, phone = phone)
            )
            if (response.ok && response.data != null) {
                val newToken = response.data.token
                if (newToken != null) TokenManager.setToken(newToken)
                TokenManager.loadUser()?.let { cached ->
                    TokenManager.saveUser(cached.copy(role = role))
                }
                Result.success(newToken)
            } else {
                Result.failure(Exception(response.error ?: "Failed to update role"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConsents(consents: DpdpConsents): Result<String?> {
        return try {
            val response = apiClient.authApi.setupConsents(
                SetupConsentsRequest(
                    dataStorage = consents.storeRecords,
                    labResults = consents.shareWithDoctor,
                    bloodDonation = consents.bloodNetwork,
                )
            )
            if (response.ok && response.data != null) {
                val newToken = response.data.token
                if (newToken != null) TokenManager.setToken(newToken)
                TokenManager.loadUser()?.let { cached ->
                    TokenManager.saveUser(cached.copy(dpdpConsents = consents))
                }
                Result.success(newToken)
            } else {
                Result.failure(Exception(response.error ?: "Failed to update consents"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves the session against the server.
     * - Fresh data on success (snapshot persisted for cold starts).
     * - [SessionExpiredException] when the token is rejected -> caller must log out.
     * - Cached user on transient failures so the app still routes correctly offline.
     */
    suspend fun getCurrentUser(): Result<User> {
        return try {
            val response = apiClient.authApi.getConfig()
            if (response.ok && response.data != null) {
                val cfg = response.data
                val user = User(
                    uid = cfg.uid ?: "",
                    email = cfg.email ?: "",
                    displayName = cfg.name ?: "",
                    role = cfg.role?.let {
                        try { UserRole.valueOf(it) } catch (_: Exception) { null }
                    },
                    isOnboarded = cfg.hasRole && cfg.hasConsents,
                    dpdpConsents = if (cfg.hasConsents) {
                        DpdpConsents(
                            storeRecords = true,
                            shareWithDoctor = true,
                            bloodNetwork = true,
                        )
                    } else DpdpConsents(),
                )
                TokenManager.saveUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception(response.error ?: "Failed to get config"))
            }
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                Result.failure(SessionExpiredException())
            } else {
                cachedUserFallback(e)
            }
        } catch (e: Exception) {
            cachedUserFallback(e)
        }
    }

    private fun cachedUserFallback(e: Exception): Result<User> {
        val cached = TokenManager.loadUser()
        return if (cached != null && TokenManager.isLoggedIn) {
            Result.success(cached)
        } else {
            Result.failure(e)
        }
    }
}

class SessionExpiredException : Exception("Session expired")
