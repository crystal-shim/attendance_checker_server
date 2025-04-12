package com.attendance.services

import com.attendance.database.DatabaseFactory.dbQuery
import com.attendance.models.Schedules
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer

object SchedulerService {
    private var timer: Timer? = null

    fun start() {
        timer = fixedRateTimer(name = "schedule-checker", period = 60000) { // Check every minute
            runBlocking {
                checkUpcomingSchedules()
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
                val scheduleTime = row[Schedules.scheduledTime]
                
                // Generate QR code
                val qrContent = "attendance_${scheduleId}_${scheduleTime}"
                val qrCode = QRCodeService.generateQRCode(qrContent)
                
                // Update database
                Schedules.update({ Schedules.id eq scheduleId }) {
                    it[Schedules.qrCode] = qrCode
                    it[Schedules.isNotified] = true
                }
                
                // Send to KakaoTalk (This is a placeholder - you'll need to implement actual Kakao API integration)
                sendToKakaoTalk(qrCode)
            }
        }
    }

    private fun sendToKakaoTalk(qrCode: String) {
        // TODO: Implement Kakao API integration
        println("Sending QR code to KakaoTalk: $qrCode")
    }

    fun stop() {
        timer?.cancel()
        timer = null
    }
} 