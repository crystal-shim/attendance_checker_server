package com.attendance.services

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

class AttendanceService(
    private val googleFormsService: GoogleFormsService,
    private val notionService: NotionService
) {
    suspend fun createAttendanceForm(title: String, scheduledTime: LocalDateTime): FormUrls = try {
        // Google Form 생성
        val urls = googleFormsService.createAttendanceForm(0, title)
        // Notion 페이지 생성
        notionService.createAttendancePage(
            title = title,
            formUrl = urls.formUrl,
            responseUrl = urls.responseUrl,
            scheduledTime = scheduledTime
        )
        urls
    } catch (e: Exception) {
        println(e)
        throw e
    }
}