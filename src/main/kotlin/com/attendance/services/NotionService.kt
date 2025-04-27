package com.attendance.services

import com.attendance.models.Schedule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

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

    suspend fun getSchedule(id: String): Schedule {
        val body = buildJsonObject {
            put("filter", buildJsonObject {
                put("property", "id")
                put("rich_text", buildJsonObject {
                    put("equals", id)
                })
            })
        }
        val response = client.post("$NOTION_API_URL/databases/$databaseId/query") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            header("Notion-Version", NOTION_VERSION)
            setBody(body)
        }
        return try {
            if (response.status != HttpStatusCode.OK) {
                throw Exception("Failed to get schedule: ${response.status}")
            }
            val json = Json.parseToJsonElement(response.bodyAsText())
            val scheduleJson = json.jsonObject["results"]?.jsonArray?.firstOrNull()
                ?: throw IllegalStateException("No results found")
            scheduleJson.toSchedule()
        } catch (e: Exception) {
            println("Error get schedule: ${e.message}")
            throw e
        }
    }

    suspend fun createAttendancePage(
        title: String,
        formUrl: String,
        responseUrl: String,
        scheduledTime: LocalDateTime
    ) {
        val id = UUID.randomUUID().toString()
        println("createAttendancePage() id: $id")
        val body = buildJsonObject {
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
                put("QR URL", buildJsonObject {
                    put("url", createQRUrl(id))
                })
                put("Form URL", buildJsonObject {
                    put("url", formUrl)
                })
                put("Response URL", buildJsonObject {
                    put("url", responseUrl)
                })
                put("Date", buildJsonObject {
                    put("date", buildJsonObject {
                        put("start", scheduledTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    })
                })
                put("id", buildJsonObject {
                    put("rich_text", buildJsonArray {
                        add(buildJsonObject {
                            put("text", buildJsonObject {
                                put("content", id)
                            })
                        })
                    })
                })
            })
        }
        println("createAttendancePage() body: $body")

        client.post("$NOTION_API_URL/pages") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            header("Notion-Version", NOTION_VERSION)
            setBody(body)
        }
    }

    private fun JsonElement.toSchedule(): Schedule {
        val obj = this.jsonObject
        val properties = obj["properties"]?.jsonObject ?: throw IllegalStateException("Missing properties")

        val id = properties["id"]
            ?.jsonObject?.get("rich_text")
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?: throw IllegalStateException("Missing id")

        val title = properties["Name"]
            ?.jsonObject?.get("title")
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?: throw IllegalStateException("Missing title")

        val scheduledTime = properties["Date"]
            ?.jsonObject?.get("date")
            ?.jsonObject?.get("start")
            ?.jsonPrimitive?.content
            ?: throw IllegalStateException("Missing scheduledTime")

        val formUrl = properties["Form URL"]
            ?.jsonObject?.get("url")
            ?.jsonPrimitive?.content
            ?: throw IllegalStateException("Missing formUrl")

        val responseUrl = properties["Response URL"]
            ?.jsonObject?.get("url")
            ?.jsonPrimitive?.content
            ?: throw IllegalStateException("Missing responseUrl")

        return Schedule(
            id = id,
            title = title,
            scheduledTime = scheduledTime,
            formUrl = formUrl,
            responseUrl = responseUrl
        )
    }

    private fun createQRUrl(scheduleId: String) =
        "https://www.elrc.run/detail?id=$scheduleId"

    companion object {
        private const val NOTION_API_URL = "https://api.notion.com/v1"
        private const val NOTION_VERSION = "2022-06-28"
    }
}