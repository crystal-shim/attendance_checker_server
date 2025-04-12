package com.attendance.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import com.attendance.models.Schedule
import com.attendance.database.DatabaseFactory.dbQuery
import com.attendance.models.Schedules
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime

fun Application.configureRouting() {
    routing {
        // Create new schedule
        post("/schedule") {
            val schedule = call.receive<Schedule>()
            val result = dbQuery {
                Schedules.insert {
                    it[title] = schedule.title
                    it[scheduledTime] = LocalDateTime.parse(schedule.scheduledTime)
                }.resultedValues?.firstOrNull()?.let { row ->
                    Schedule(
                        id = row[Schedules.id],
                        title = row[Schedules.title],
                        scheduledTime = row[Schedules.scheduledTime].toString(),
                        qrCode = row[Schedules.qrCode],
                        isNotified = row[Schedules.isNotified]
                    )
                }
            }
            call.respond(result ?: error("Failed to create schedule"))
        }

        // Get all schedules
        get("/schedules") {
            val schedules = dbQuery {
                Schedules.selectAll().map { row ->
                    Schedule(
                        id = row[Schedules.id],
                        title = row[Schedules.title],
                        scheduledTime = row[Schedules.scheduledTime].toString(),
                        qrCode = row[Schedules.qrCode],
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
                        qrCode = row[Schedules.qrCode],
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