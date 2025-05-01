package com.attendance

import com.attendance.env.EnvLoader
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import com.attendance.plugins.*
import com.attendance.models.Schedule
import com.attendance.services.GoogleFormsService
import com.attendance.services.NotionService
import com.attendance.services.SchedulerService
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import java.io.File
import com.attendance.services.AttendanceService

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Install CORS
    install(CORS) {
        // 로컬 개발 환경 
        allowHost("localhost:3000")
        allowHost("127.0.0.1:3000")
        
        // 프로덕션 환경
        allowHost("www.elrc.run", schemes = listOf("https"))
        
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

    // Configure OpenAPI documentation
    configureOpenAPI()

    val envLoader = EnvLoader()

    val googleFormsService = GoogleFormsService(
        clientId = envLoader.readEnv("GOOGLE_CLIENT_ID"),
        clientSecret = envLoader.readEnv("GOOGLE_CLIENT_SECRET"),
        refreshToken = envLoader.readEnv("GOOGLE_REFRESH_TOKEN")
    )
    val notionService = NotionService(
        token = envLoader.readEnv("NOTION_API_TOKEN"),
        databaseId = envLoader.readEnv("NOTION_DATABASE_ID")
    )
    val attendanceService = AttendanceService(googleFormsService, notionService)
    val schedulerService = SchedulerService(attendanceService)

    configureRouting(attendanceService, notionService, schedulerService)
    schedulerService.start()
} 