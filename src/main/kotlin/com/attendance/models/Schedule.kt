package com.attendance.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Schedule(
    val id: Int = 0,
    val title: String,
    val scheduledTime: String,
    val formUrl: String? = null,
    val isNotified: Boolean = false
)

object Schedules : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 128)
    val scheduledTime = datetime("scheduled_time")
    val formUrl = varchar("form_url", 512).nullable()
    val isNotified = bool("is_notified").default(false)

    override val primaryKey = PrimaryKey(id)
} 