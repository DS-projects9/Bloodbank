package com.medvault.data.remote

data class ApiResponse<T>(
    val ok: Boolean = true,
    val data: T? = null,
    val error: String? = null
)
