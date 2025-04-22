package com.attendance.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class NotionService(
    private val token: String,
    private val databaseId: String
) {
    private val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun createAttendancePage(title: String, formUrl: String) {
        client.post("$NOTION_API_URL/pages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            header("Notion-Version", NOTION_VERSION)
            setBody(buildJsonObject {
                put("parent", buildJsonObject {
                    put("database_id", databaseId)
                })
                put("properties", buildJsonObject {
                    put("Name", buildJsonObject {
                        put("title", buildJsonArray {
                            add(buildJsonObject {
                                put("text", buildJsonObject {
                                    put("content", title)
                                })
                            })
                        })
                    })
                    put("Form URL", buildJsonObject {
                        put("url", formUrl)
                    })
                })
            })
        }
    }

    companion object {
        private const val NOTION_API_URL = "https://api.notion.com/v1"
        private const val NOTION_VERSION = "2022-06-28"
    }
}