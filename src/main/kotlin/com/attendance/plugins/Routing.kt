package com.attendance.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import com.attendance.models.Schedule
import com.attendance.models.ScheduleRequest
import com.attendance.services.AttendanceService
import java.time.LocalDateTime

fun Application.configureRouting(attendanceService: AttendanceService) {
    routing {
        // Create new schedule
        post("/schedule") {
            val request = call.receive<ScheduleRequest>()
            val urls = attendanceService.createAttendanceForm(request.title, LocalDateTime.parse(request.scheduledTime))
            call.respond(Schedule(
                title = request.title,
                scheduledTime = request.scheduledTime,
                formUrl = urls.formUrl,
                responseUrl = urls.responseUrl
            ))
        }
    }
} 