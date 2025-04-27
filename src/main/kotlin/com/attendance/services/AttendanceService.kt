package com.attendance.services

import com.attendance.models.Schedule
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class AttendanceService(
    private val googleFormsService: GoogleFormsService,
    private val notionService: NotionService
) {
    suspend fun createAttendanceForm(title: String, scheduledTime: LocalDateTime): Schedule? = try {
        // Google Form 생성
        val id = UUID.randomUUID().toString()
        val urls = googleFormsService.createAttendanceForm(title, scheduledTime)
        val schedule = Schedule(
            id = id,
            title = title,
            formUrl = urls.formUrl,
            responseUrl = urls.responseUrl,
            scheduledTime = scheduledTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        // Notion 페이지 생성
        notionService.createAttendancePage(schedule)
        schedule
    } catch (e: Exception) {
        println(e)
        throw e
    }
}