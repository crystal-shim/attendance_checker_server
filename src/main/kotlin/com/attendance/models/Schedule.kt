package com.attendance.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Schedule(
    val id: Int = 0,
    val title: String,
    val scheduledTime: String,
    val formUrl: String? = null
) 