package com.attendance.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Schedule(
    val id: String,
    val title: String,
    val scheduledTime: String,
    val formUrl: String,
    val responseUrl: String
) 