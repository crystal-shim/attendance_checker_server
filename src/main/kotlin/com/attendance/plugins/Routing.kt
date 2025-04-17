package com.attendance.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import com.attendance.models.Schedule
import com.attendance.database.DatabaseFactory.dbQuery
import com.attendance.models.Schedules
import com.attendance.services.GoogleFormsService
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime

fun Application.configureRouting(googleFormsService: GoogleFormsService) {
    routing {
        // Create new schedule
        post("/schedule") {
            val schedule = call.receive<Schedule>()
            val result = dbQuery {
                // Insert schedule first to get the ID
                val insertedRow = Schedules.insert {
                    it[title] = schedule.title
                    it[scheduledTime] = LocalDateTime.parse(schedule.scheduledTime)
                }.resultedValues?.firstOrNull() ?: error("Failed to create schedule")

                // Create Google Form
                val formUrl = googleFormsService.createAttendanceForm(insertedRow[Schedules.id], insertedRow[Schedules.title])

                // Update the schedule with form URL
                Schedules.update({ Schedules.id eq insertedRow[Schedules.id] }) {
                    it[Schedules.formUrl] = formUrl
                }

                // Return the complete schedule
                Schedule(
                    id = insertedRow[Schedules.id],
                    title = insertedRow[Schedules.title],
                    scheduledTime = insertedRow[Schedules.scheduledTime].toString(),
                    formUrl = formUrl,
                    isNotified = insertedRow[Schedules.isNotified]
                )
            }
            call.respond(result)
        }

        // Get all schedules
        get("/schedules") {
            val schedules = dbQuery {
                Schedules.selectAll().map { row ->
                    Schedule(
                        id = row[Schedules.id],
                        title = row[Schedules.title],
                        scheduledTime = row[Schedules.scheduledTime].toString(),
                        formUrl = row[Schedules.formUrl],
                        isNotified = row[Schedules.isNotified]
                    )
                }
            }
            call.respond(schedules)
        }

        // Get specific schedule
        get("/schedule/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: error("Invalid ID")
            val schedule = dbQuery {
                Schedules.select { Schedules.id eq id }.map { row ->
                    Schedule(
                        id = row[Schedules.id],
                        title = row[Schedules.title],
                        scheduledTime = row[Schedules.scheduledTime].toString(),
                        formUrl = row[Schedules.formUrl],
                        isNotified = row[Schedules.isNotified]
                    )
                }.singleOrNull()
            }
            if (schedule != null) {
                call.respond(schedule)
            } else {
                call.respondText("Schedule not found", status = io.ktor.http.HttpStatusCode.NotFound)
            }
        }
    }
} 