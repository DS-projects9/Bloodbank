package com.medkeen.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val ok: Boolean = true,
    val data: T? = null,
    val error: String? = null,
)

fun <T> success(data: T) = ApiResponse(ok = true, data = data)
fun error(message: String) = ApiResponse<Unit>(ok = false, error = message)
