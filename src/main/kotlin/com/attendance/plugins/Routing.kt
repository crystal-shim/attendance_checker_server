package com.attendance.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import com.attendance.models.Schedule
import com.attendance.services.GoogleFormsService
import java.time.LocalDateTime

fun Application.configureRouting(googleFormsService: GoogleFormsService) {
    routing {
        // Create new schedule
        post("/schedule") {
            val schedule = call.receive<Schedule>()
            val formUrl = googleFormsService.createAttendanceForm(0, schedule.title)
            call.respond(Schedule(
                title = schedule.title,
                scheduledTime = schedule.scheduledTime,
                formUrl = formUrl
            ))
        }
    }
} 