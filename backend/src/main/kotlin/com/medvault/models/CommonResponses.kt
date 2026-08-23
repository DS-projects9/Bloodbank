package com.medvault.models

import kotlinx.serialization.Serializable

@Serializable
data class OkResponse(val ok: Boolean = true)

@Serializable
data class VerifiedResponse(val verified: Boolean, val token: String? = null)

@Serializable
data class SlotsCreatedResponse(val slotsCreated: Int)

@Serializable
data class UrlResponse(val url: String, val expiresInMinutes: Int)
