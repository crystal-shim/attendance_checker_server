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
import io.ktor.http.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()

    // Install CORS
    install(CORS) {
        // 로컬 개발 환경 
        allowHost("localhost:3000")
        allowHost("127.0.0.1:3000")
        
        // 프로덕션 환경
        allowHost("www.elrc.run")
        
        // HTTP 메서드 허용
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        
        // 필요한 헤더 허용
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        
        // 자격 증명 허용 (쿠키 등)
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