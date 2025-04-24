package com.attendance.services

import kotlinx.coroutines.*
import java.time.*
import kotlin.time.Duration.Companion.minutes

class SchedulerService(
    private val googleFormsService: GoogleFormsService, 
    private val notionService: NotionService
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val checkInterval = 1.minutes
    private val koreaZoneId = ZoneId.of("Asia/Seoul")
    private var lastWednesdayCreationDate: LocalDate? = null
    private var lastSaturdayCreationDate: LocalDate? = null

    fun start() {
        scope.launch {
            while (isActive) {
                createRegularSchedules()
                delay(checkInterval)
            }
        }
    }

    internal suspend fun createRegularSchedules() {
        val now = ZonedDateTime.now(koreaZoneId)
        val today = now.toLocalDate()
        
        // 다음 스케줄 시간 계산
        val nextSchedule = getNextScheduleTime(now)
        if (nextSchedule != null) {
            val isWednesday = nextSchedule.dayOfWeek == DayOfWeek.WEDNESDAY
            val lastCreationDate = if (isWednesday) lastWednesdayCreationDate else lastSaturdayCreationDate
            
            // 오늘 해당 요일의 스케줄을 아직 생성하지 않았다면 생성
            if (lastCreationDate != today) {
                createSchedule(nextSchedule)
                if (isWednesday) {
                    lastWednesdayCreationDate = today
                } else {
                    lastSaturdayCreationDate = today
                }
            }
        }
    }

    private fun getNextScheduleTime(now: ZonedDateTime): ZonedDateTime? {
        val wednesday = createNextScheduleTime(now, DayOfWeek.WEDNESDAY, 20, 30)
        val saturday = createNextScheduleTime(now, DayOfWeek.SATURDAY, 8, 0)
        
        // 둘 다 미래 시간인 경우, 더 가까운 시간 선택
        return when {
            wednesday.isAfter(now) && saturday.isAfter(now) -> if (wednesday.isBefore(saturday)) wednesday else saturday
            wednesday.isAfter(now) -> wednesday
            saturday.isAfter(now) -> saturday
            else -> null
        }
    }

    private fun createNextScheduleTime(
        now: ZonedDateTime,
        targetDay: DayOfWeek,
        hour: Int,
        minute: Int
    ): ZonedDateTime {
        var scheduleTime = now.with(targetDay)
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        // 이미 지난 시간이면 다음 주로
        if (scheduleTime.isBefore(now)) {
            scheduleTime = scheduleTime.plusWeeks(1)
        }

        return scheduleTime
    }

    internal suspend fun createSchedule(scheduleTime: ZonedDateTime) {
        val title = when (scheduleTime.dayOfWeek) {
            DayOfWeek.WEDNESDAY -> "수요일 저녁 출석체크"
            DayOfWeek.SATURDAY -> "토요일 오전 출석체크"
            else -> "출석체크"
        }

        // Google Form 생성
        val (formUrl, responseUrl) = googleFormsService.createAttendanceForm(0, title)
        
        // Notion 페이지 생성
        notionService.createAttendancePage(title, formUrl, responseUrl)
    }

    // For testing purposes
    internal fun resetLastCreationDates() {
        lastWednesdayCreationDate = null
        lastSaturdayCreationDate = null
    }
} 