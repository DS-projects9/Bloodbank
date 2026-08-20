package com.medvault.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class SlotUpdate(
    val day: String,
    val startTime: String,
    val endTime: String,
    val slotMinutes: Int,
)

@Serializable
data class UpdateDoctorScheduleRequest(
    val slots: List<SlotUpdate>,
)

@Serializable
data class PublishNextWeekRequest(
    val weekStart: String,
    val slots: List<SlotUpdate>? = null,
)
