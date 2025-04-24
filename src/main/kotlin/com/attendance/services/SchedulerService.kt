package com.attendance.services

import com.attendance.models.RegularSchedule
import com.attendance.models.Schedule
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
            
            createSchedulesForDateRange(nextWeekStart, nextWeekEnd)
            
            lastBatchCreationDate = today
            println("[Scheduler] Batch creation completed. Next batch will be on: ${today.plusDays(7)}")
        }
    }

    /**
     * 오늘부터 다음주 일요일까지의 스케줄을 수동으로 생성합니다.
     * @return 생성된 스케줄 목록
     */
    suspend fun createSchedulesUntilNextSunday(): List<Schedule> {
        val now = ZonedDateTime.now(koreaZoneId)
        val today = now.toLocalDate()
        
        // 다음주 일요일 찾기
        var nextSunday = today
        while (nextSunday.dayOfWeek != DayOfWeek.SUNDAY) {
            nextSunday = nextSunday.plusDays(1)
        }
        
        println("[Scheduler] Manual batch creation from $today to $nextSunday")
        return createSchedulesForDateRange(today, nextSunday)
    }

    private suspend fun createSchedulesForDateRange(startDate: LocalDate, endDate: LocalDate): List<Schedule> {
        val createdSchedules = mutableListOf<Schedule>()
        
        println("[Scheduler] Date range: $startDate ~ $endDate")
        
        regularSchedules.forEach { schedule ->
            // 해당 기간 내의 요일 찾기
            var targetDate = startDate
            while (targetDate <= endDate) {
                if (targetDate.dayOfWeek == schedule.dayOfWeek) {
                    val scheduleDateTime = ZonedDateTime.of(
                        targetDate,
                        schedule.time,
                        koreaZoneId
                    )
                    println("[Scheduler] Creating schedule for: $scheduleDateTime - ${schedule.title}")
                    val createdSchedule = createSchedule(scheduleDateTime, schedule.title)
                    createdSchedule?.let { createdSchedules.add(it) }
                }
                targetDate = targetDate.plusDays(1)
            }
        }
        
        return createdSchedules
    }

    private suspend fun createSchedule(scheduleTime: ZonedDateTime, title: String): Schedule? {
        return try {
            val urls = attendanceService.createAttendanceForm(title, scheduleTime.toLocalDateTime())
            Schedule(
                title = title,
                scheduledTime = scheduleTime.toLocalDateTime().toString(),
                formUrl = urls.formUrl,
                responseUrl = urls.responseUrl
            )
        } catch (e: Exception) {
            println("[Scheduler] Failed to create schedule: ${e.message}")
            null
        }
    }

    // For testing purposes
    internal fun resetLastCreationDate() {
        lastBatchCreationDate = null
    }
} 