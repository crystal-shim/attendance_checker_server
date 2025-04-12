package com.attendance

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.attendance.plugins.*
import com.attendance.models.DatabaseFactory
import com.attendance.models.Schedule
import com.attendance.services.GoogleFormsService
import com.attendance.services.SchedulerService
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()

    // Install plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    val googleFormsService = GoogleFormsService()
    val schedulerService = SchedulerService(googleFormsService)
    schedulerService.start()

    // Configure routes
    configureRouting()
} 