package com.attendance.models

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleRequest(
    val title: String,
    val scheduledTime: String
) 