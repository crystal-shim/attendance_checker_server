package com.attendance.services

import java.time.LocalDateTime
import java.time.ZonedDateTime

class AttendanceService(
    private val googleFormsService: GoogleFormsService,
    private val notionService: NotionService
) {
    suspend fun createAttendanceForm(title: String, scheduledTime: LocalDateTime): FormUrls {
        // Google Form 생성
        val urls = googleFormsService.createAttendanceForm(0, title)
        
        // Notion 페이지 생성
        notionService.createAttendancePage(
            title = title,
            formUrl = urls.formUrl,
            responseUrl = urls.responseUrl,
            scheduledTime = scheduledTime
        )

        return urls
    }
} 