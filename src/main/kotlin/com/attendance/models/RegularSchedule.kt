package com.attendance.models

import java.time.DayOfWeek
import java.time.LocalTime

data class RegularSchedule(
    val dayOfWeek: DayOfWeek,
    val time: LocalTime,
    val title: String
) 