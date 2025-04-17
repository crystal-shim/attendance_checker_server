package com.attendance.services

import com.attendance.database.DatabaseFactory.dbQuery
import com.attendance.models.Schedules
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

class SchedulerService(private val googleFormsService: GoogleFormsService) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val checkInterval = 1.minutes

    fun start() {
        scope.launch {
            while (isActive) {
                checkUpcomingSchedules()
                delay(checkInterval)
            }
        }
    }

    private suspend fun checkUpcomingSchedules() {
        val now = LocalDateTime.now()
        val oneHourLater = now.plusHours(1)

        dbQuery {
            Schedules.select {
                (Schedules.scheduledTime greater now) and
                (Schedules.scheduledTime lessEq oneHourLater) and
                (Schedules.isNotified eq false)
            }.forEach { row ->
                val scheduleId = row[Schedules.id]
                val formUrl = row[Schedules.formUrl] ?: return@forEach
                
                // URL이 이미 있으므로 알림 상태만 업데이트
                Schedules.update({ Schedules.id eq scheduleId }) {
                    it[Schedules.isNotified] = true
                }

                // 카카오톡으로 URL 전송
                sendToKakaoTalk(formUrl)
            }
        }
    }

    private fun sendToKakaoTalk(formUrl: String) {
        // 카카오톡 메시지 전송 로직 구현
        println("Sending form URL to KakaoTalk: $formUrl")
    }
} 