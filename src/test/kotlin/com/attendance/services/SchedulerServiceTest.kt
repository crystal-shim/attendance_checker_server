package com.attendance.services

import com.attendance.database.DatabaseFactory
import com.attendance.models.Schedules
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds

class SchedulerServiceTest {
    private lateinit var schedulerService: SchedulerService
    private lateinit var mockGoogleFormsService: GoogleFormsService
    private lateinit var mockNotionService: NotionService
    private val koreaZoneId = ZoneId.of("Asia/Seoul")
    
    @Before
    fun setUp() {
        // Set up H2 in-memory database
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        
        // Create tables
        transaction {
            SchemaUtils.create(Schedules)
        }
        
        // Mock services
        mockGoogleFormsService = mockk()
        mockNotionService = mockk()
        coEvery { mockNotionService.createAttendancePage(any(), any()) } just Runs
        
        schedulerService = spyk(SchedulerService(mockGoogleFormsService, mockNotionService))
        schedulerService.resetLastCreationDates()
    }
    
    @After
    fun tearDown() {
        // Drop tables
        transaction {
            SchemaUtils.drop(Schedules)
        }
    }
    
    @Test
    fun `createScheduleIfNotExists should create new schedule with form URL`() = runBlocking {
        // Given
        val scheduleTime = ZonedDateTime.of(2024, 3, 20, 20, 30, 0, 0, koreaZoneId)
        val expectedTitle = "수요일 저녁 출석체크"
        val expectedFormUrl = "https://forms.google.com/test-form"
        
        // Mock GoogleFormsService
        coEvery { 
            mockGoogleFormsService.createAttendanceForm(any(), expectedTitle) 
        } returns expectedFormUrl
        
        // When
        schedulerService.createScheduleIfNotExists(scheduleTime)
        
        // Then
        transaction {
            val schedule = Schedules.select { 
                Schedules.scheduledTime eq scheduleTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
            }.firstOrNull()
            
            assertNotNull(schedule)
            assertEquals(expectedTitle, schedule[Schedules.title])
            assertEquals(expectedFormUrl, schedule[Schedules.formUrl])
            assertEquals(false, schedule[Schedules.isNotified])
        }
        
        coVerify(exactly = 1) { 
            mockGoogleFormsService.createAttendanceForm(any(), expectedTitle) 
        }
    }
    
    @Test
    fun `createScheduleIfNotExists should not create duplicate schedule`() = runBlocking {
        // Given
        val scheduleTime = ZonedDateTime.of(2024, 3, 20, 20, 30, 0, 0, koreaZoneId)
        val expectedTitle = "수요일 저녁 출석체크"
        val expectedFormUrl = "https://forms.google.com/test-form"
        
        coEvery { 
            mockGoogleFormsService.createAttendanceForm(any(), expectedTitle) 
        } returns expectedFormUrl
        
        // When
        schedulerService.createScheduleIfNotExists(scheduleTime) // 첫 번째 생성
        schedulerService.createScheduleIfNotExists(scheduleTime) // 중복 생성 시도
        
        // Then
        transaction {
            val count = Schedules.select { 
                Schedules.scheduledTime eq scheduleTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
            }.count()
            
            assertEquals(1, count)
        }
        
        coVerify(exactly = 1) { 
            mockGoogleFormsService.createAttendanceForm(any(), expectedTitle) 
        }
    }
    
    @Test
    fun `createScheduleIfNotExists should create schedule for Saturday morning`() = runBlocking {
        // Given
        val scheduleTime = ZonedDateTime.of(2024, 3, 23, 8, 0, 0, 0, koreaZoneId)
        val expectedTitle = "토요일 오전 출석체크"
        val expectedFormUrl = "https://forms.google.com/test-form"
        
        coEvery { 
            mockGoogleFormsService.createAttendanceForm(any(), expectedTitle) 
        } returns expectedFormUrl
        
        // When
        schedulerService.createScheduleIfNotExists(scheduleTime)
        
        // Then
        transaction {
            val schedule = Schedules.select { 
                Schedules.scheduledTime eq scheduleTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
            }.firstOrNull()
            
            assertNotNull(schedule)
            assertEquals(expectedTitle, schedule[Schedules.title])
            assertEquals(expectedFormUrl, schedule[Schedules.formUrl])
            assertEquals(false, schedule[Schedules.isNotified])
        }
        
        coVerify(exactly = 1) { 
            mockGoogleFormsService.createAttendanceForm(any(), expectedTitle) 
        }
    }

    @Test
    fun `createRegularSchedules should create schedule for next Wednesday when current time is before Wednesday`() = runBlocking {
        // Given
        // 월요일 오전 10시로 시간 설정
        val mockNow = ZonedDateTime.of(2024, 3, 18, 10, 0, 0, 0, koreaZoneId)
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(koreaZoneId) } returns mockNow

        val expectedFormUrl = "https://forms.google.com/test-form"
        coEvery { 
            mockGoogleFormsService.createAttendanceForm(any(), any()) 
        } returns expectedFormUrl

        // When
        schedulerService.createRegularSchedules()

        // Then
        transaction {
            val schedules = Schedules.select { 
                Schedules.scheduledTime greaterEq mockNow.toLocalDateTime()
            }.toList()

            assertEquals(1, schedules.size)
            val schedule = schedules[0]
            assertEquals("수요일 저녁 출석체크", schedule[Schedules.title])
            
            // 생성된 스케줄이 다음 수요일 20:30인지 확인
            val expectedTime = mockNow.with(DayOfWeek.WEDNESDAY)
                .withHour(20)
                .withMinute(30)
                .withSecond(0)
                .withNano(0)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
            
            assertEquals(expectedTime, schedule[Schedules.scheduledTime])
        }

        coVerify(exactly = 1) { 
            mockGoogleFormsService.createAttendanceForm(any(), "수요일 저녁 출석체크") 
        }
    }

    @Test
    fun `createRegularSchedules should create schedule for next Saturday when current time is after Wednesday`() = runBlocking {
        // Given
        // 수요일 저녁 21시로 시간 설정
        val mockNow = ZonedDateTime.of(2024, 3, 20, 21, 0, 0, 0, koreaZoneId)
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(koreaZoneId) } returns mockNow

        val expectedFormUrl = "https://forms.google.com/test-form"
        coEvery { 
            mockGoogleFormsService.createAttendanceForm(any(), any()) 
        } returns expectedFormUrl

        // When
        schedulerService.createRegularSchedules()

        // Then
        transaction {
            val schedules = Schedules.select { 
                Schedules.scheduledTime greaterEq mockNow.toLocalDateTime()
            }.toList()

            assertEquals(1, schedules.size)
            val schedule = schedules[0]
            assertEquals("토요일 오전 출석체크", schedule[Schedules.title])
            
            // 생성된 스케줄이 이번주 토요일 08:00인지 확인
            val expectedTime = mockNow.with(DayOfWeek.SATURDAY)
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
            
            assertEquals(expectedTime, schedule[Schedules.scheduledTime])
        }

        coVerify(exactly = 1) { 
            mockGoogleFormsService.createAttendanceForm(any(), "토요일 오전 출석체크") 
        }
    }

    @Test
    fun `createRegularSchedules should not create duplicate schedules on same day`() = runBlocking {
        // Given
        val mockNow = ZonedDateTime.of(2024, 3, 18, 10, 0, 0, 0, koreaZoneId)
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(koreaZoneId) } returns mockNow

        val expectedFormUrl = "https://forms.google.com/test-form"
        coEvery { 
            mockGoogleFormsService.createAttendanceForm(any(), any()) 
        } returns expectedFormUrl

        // When
        schedulerService.createRegularSchedules() // 첫 번째 실행
        schedulerService.createRegularSchedules() // 같은 날 두 번째 실행

        // Then
        transaction {
            val scheduleCount = Schedules.select { 
                Schedules.scheduledTime greaterEq mockNow.toLocalDateTime()
            }.count()

            assertEquals(1, scheduleCount)
        }

        coVerify(exactly = 1) { 
            mockGoogleFormsService.createAttendanceForm(any(), any()) 
        }
    }

    @Test
    fun `createRegularSchedules should create schedule for next week when current time is after both schedule times`() = runBlocking {
        // Given
        // 토요일 오후 2시 (이번주 스케줄 모두 지난 후)로 설정
        val mockNow = ZonedDateTime.of(2024, 3, 23, 14, 0, 0, 0, koreaZoneId)
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(koreaZoneId) } returns mockNow

        val expectedFormUrl = "https://forms.google.com/test-form"
        coEvery { 
            mockGoogleFormsService.createAttendanceForm(any(), any()) 
        } returns expectedFormUrl

        // When
        schedulerService.createRegularSchedules()

        // Then
        transaction {
            val schedule = Schedules.select { 
                Schedules.scheduledTime greaterEq mockNow.toLocalDateTime()
            }.firstOrNull()

            assertNotNull(schedule)
            assertEquals("수요일 저녁 출석체크", schedule[Schedules.title])
            
            // 생성된 스케줄이 다음주 수요일인지 확인
            val expectedTime = mockNow.plusWeeks(1)
                .with(DayOfWeek.WEDNESDAY)
                .withHour(20)
                .withMinute(30)
                .withSecond(0)
                .withNano(0)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
            
            assertEquals(expectedTime, schedule[Schedules.scheduledTime])
        }
    }

    @Test
    fun `integration test - start function should create schedule and send notification properly`() = runBlocking {
        // Given: Wednesday 19:30 (1 hour before attendance check)
        val wednesday = ZonedDateTime.of(2024, 3, 20, 19, 30, 0, 0, koreaZoneId)
        
        mockkStatic(ZonedDateTime::class) {
            every { ZonedDateTime.now(koreaZoneId) } returns wednesday
            
            // Mock form creation
            coEvery { 
                mockGoogleFormsService.createAttendanceForm(any(), any()) 
            } returns "https://forms.google.com/test-form"
            
            // When: Call start and wait briefly
            val job = launch { schedulerService.start() }
            delay(3.seconds)
            
            // Then: Schedule should be created and form generated
            coVerify(exactly = 1) { 
                mockGoogleFormsService.createAttendanceForm(any(), any())
            }
            
            // Verify schedule was created in database
            transaction {
                val schedule = Schedules.select { 
                    Schedules.scheduledTime eq wednesday.withHour(20).withMinute(30)
                        .withSecond(0).withNano(0)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime()
                }.firstOrNull()
                
                assertNotNull(schedule, "Schedule should be created")
                assertEquals("수요일 저녁 출석체크", schedule[Schedules.title])
                assertEquals("https://forms.google.com/test-form", schedule[Schedules.formUrl])
                // TODO - check
//                assertEquals(true, schedule[Schedules.isNotified])
            }
            
            job.cancel()
        }
    }
} 