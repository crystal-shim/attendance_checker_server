package com.attendance.services

import com.attendance.models.RegularSchedule
import kotlinx.coroutines.*
import java.time.*
import kotlin.time.Duration.Companion.hours

class SchedulerService(
    private val attendanceService: AttendanceService
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val checkInterval = 1.hours
    private val koreaZoneId = ZoneId.of("Asia/Seoul")
    private var lastBatchCreationDate: LocalDate? = null

    // 정기 스케줄 목록
    private val regularSchedules = listOf(
        RegularSchedule(
            dayOfWeek = DayOfWeek.WEDNESDAY,
            time = LocalTime.of(20, 30),
            title = "수요일 저녁 출석체크"
        ),
        RegularSchedule(
            dayOfWeek = DayOfWeek.SATURDAY,
            time = LocalTime.of(8, 0),
            title = "토요일 오전 출석체크"
        )
    )

    fun start() {
        scope.launch {
            while (isActive) {
                createSchedulesForNextWeek()
                delay(checkInterval)
            }
        }
    }

    internal suspend fun createSchedulesForNextWeek() {
        val now = ZonedDateTime.now(koreaZoneId)
        val today = now.toLocalDate()
        
        println("[Scheduler] Current time: $now")
        println("[Scheduler] Is Sunday: ${now.dayOfWeek == DayOfWeek.SUNDAY}")
        println("[Scheduler] Last batch creation date: $lastBatchCreationDate")
        
        // 매주 일요일에 다음 주 스케줄 생성
        if (now.dayOfWeek == DayOfWeek.SUNDAY && lastBatchCreationDate != today) {
            println("[Scheduler] Starting batch creation...")
            
            val nextWeekStart = today.plusDays(1) // 다음날(월요일)부터
            val nextWeekEnd = nextWeekStart.plusDays(6) // 다음주 일요일까지
            
            println("[Scheduler] Date range: $nextWeekStart ~ $nextWeekEnd")
            
            regularSchedules.forEach { schedule ->
                // 다음 주의 해당 요일 찾기
                var targetDate = nextWeekStart
                while (targetDate <= nextWeekEnd) {
                    println("[Scheduler] Checking date: $targetDate (${targetDate.dayOfWeek})")
                    if (targetDate.dayOfWeek == schedule.dayOfWeek) {
                        val scheduleDateTime = ZonedDateTime.of(
                            targetDate,
                            schedule.time,
                            koreaZoneId
                        )
                        println("[Scheduler] Creating schedule for: $scheduleDateTime - ${schedule.title}")
                        createSchedule(scheduleDateTime, schedule.title)
                        break
                    }
                    targetDate = targetDate.plusDays(1)
                }
            }
            
            lastBatchCreationDate = today
            println("[Scheduler] Batch creation completed. Next batch will be on: ${today.plusDays(7)}")
        }
    }

    private suspend fun createSchedule(scheduleTime: ZonedDateTime, title: String) {
        attendanceService.createAttendanceForm(title, scheduleTime.toLocalDateTime())
    }

    // For testing purposes
    internal fun resetLastCreationDate() {
        lastBatchCreationDate = null
    }
} 