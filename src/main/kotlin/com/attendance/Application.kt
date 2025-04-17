package com.attendance

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.attendance.plugins.*
import com.attendance.database.DatabaseFactory
import com.attendance.models.Schedule
import com.attendance.services.GoogleFormsService
import com.attendance.services.SchedulerService
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import io.ktor.server.plugins.cors.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()

    // Install CORS
    install(CORS) {
        allowHost("localhost:3000")
        allowHost("127.0.0.1:3000")
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowHeader("Authorization")
        allowHeader("Content-Type")
        allowCredentials = true
        maxAgeInSeconds = 3600
    }

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

    // Configure routes with GoogleFormsService
    configureRouting(googleFormsService)
} 