package com.medvault.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class BloodRequestCreate(
    val patientName: String,
    val bloodGroup: String,
    val units: Int,
    val hospitalName: String,
    val hospitalAddress: String,
    val urgency: String = "normal",
    val note: String? = null,
)

@Serializable
data class BloodFulfillRequest(
    val requestId: String,
    val units: Int,
)

@Serializable
data class DonorBookingCreate(
    val bloodGroup: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val hospitalName: String,
    val hospitalAddress: String,
    val note: String? = null,
)

@Serializable
data class BloodInventoryAdjustRequest(
    val bloodGroup: String,
    val units: Int,
    val reason: String,
)

@Serializable
data class BloodGrantFulfillRequest(
    val grantId: String,
    val units: Int,
)

@Serializable
data class BloodBankConfigRequest(
    val hospitalName: String,
    val hospitalAddress: String,
    val bankName: String? = null,
    val bloodBankLicense: String? = null,
)
