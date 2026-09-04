package com.medkeen.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class UploadInitRequest(
    val fileName: String,
    val contentType: String = "application/octet-stream",
)
