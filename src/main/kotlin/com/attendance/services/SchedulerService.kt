package com.attendance.services

import com.attendance.database.DatabaseFactory.dbQuery
import com.attendance.models.Schedules
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.*
import kotlin.time.Duration.Companion.minutes

class SchedulerService(private val googleFormsService: GoogleFormsService) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val checkInterval = 1.minutes
    private val koreaZoneId = ZoneId.of("Asia/Seoul")
    private var lastWednesdayCreationDate: LocalDate? = null
    private var lastSaturdayCreationDate: LocalDate? = null

    fun start() {
        scope.launch {
            while (isActive) {
                checkUpcomingSchedules()
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
                createScheduleIfNotExists(nextSchedule)
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

    internal suspend fun createScheduleIfNotExists(scheduleTime: ZonedDateTime) {
        val localDateTime = scheduleTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        
        dbQuery {
            val exists = Schedules.select {
                Schedules.scheduledTime eq localDateTime
            }.count() > 0

            if (!exists) {
                val title = when (scheduleTime.dayOfWeek) {
                    DayOfWeek.WEDNESDAY -> "수요일 저녁 출석체크"
                    DayOfWeek.SATURDAY -> "토요일 오전 출석체크"
                    else -> "출석체크"
                }

                // Google Form 생성
                val formUrl = googleFormsService.createAttendanceForm(0, title)

                // 스케줄 생성 (UTC 시간으로 저장)
                Schedules.insert {
                    it[Schedules.title] = title
                    it[Schedules.scheduledTime] = localDateTime
                    it[Schedules.formUrl] = formUrl
                    it[Schedules.isNotified] = false
                }
            }
        }
    }

    private suspend fun checkUpcomingSchedules() {
        val now = ZonedDateTime.now(koreaZoneId)
        val oneHourLater = now.plusHours(1)
        val nowUtc = now.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        val oneHourLaterUtc = oneHourLater.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()

        dbQuery {
            Schedules.select {
                (Schedules.scheduledTime greater nowUtc) and
                (Schedules.scheduledTime lessEq oneHourLaterUtc) and
                (Schedules.isNotified eq false)
            }.forEach { row ->
                val scheduleId = row[Schedules.id]
                
                // Update notification status
                Schedules.update({ Schedules.id eq scheduleId }) {
                    it[Schedules.isNotified] = true
                }
            }
        }
    }

    // For testing purposes
    internal fun resetLastCreationDates() {
        lastWednesdayCreationDate = null
        lastSaturdayCreationDate = null
    }
} 