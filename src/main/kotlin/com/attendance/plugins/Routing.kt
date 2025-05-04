package com.attendance.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import com.attendance.models.Schedule
import com.attendance.models.ScheduleRequest
import com.attendance.services.AttendanceService
import com.attendance.services.NotionService
import com.attendance.services.SchedulerService
import io.ktor.http.*
import io.ktor.server.util.*
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

fun Application.configureRouting(attendanceService: AttendanceService, notionService: NotionService, schedulerService: SchedulerService) {
    routing {
        // Get schedule based on id
        get("/detail") {
            try {
                val id = call.request.queryParameters["id"].toString()
                if (id.isNotBlank()) {
                    val schedule = notionService.getSchedule(id)
                    call.respond(schedule)
                } else {
                    call.respondText("id parameter not exist.", status = HttpStatusCode.BadRequest)
                }
            } catch (e: Exception) {
                call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
            }
        }

        // Create new schedule
        post("/schedule") {
            val request = call.receive<ScheduleRequest>()
            val schedule = attendanceService.createAttendanceForm(request.title, ZonedDateTime.parse(request.scheduledTime))
            if (schedule != null) {
                call.respond(Schedule(
                    id = UUID.randomUUID().toString(),
                    title = request.title,
                    scheduledTime = request.scheduledTime,
                    formUrl = schedule.formUrl,
                    responseUrl = schedule.responseUrl
                ))
            } else {
                call.respondText("Failed to create schedule", status = HttpStatusCode.InternalServerError)
            }
        }

        // Create schedules until next Sunday
        post("/schedule/batch") {
            val createdSchedules = schedulerService.createSchedulesUntilNextSunday()
            call.respond(mapOf(
                "message" to "Successfully created ${createdSchedules.size} schedules",
                "schedules" to createdSchedules
            ))
        }
    }
} 