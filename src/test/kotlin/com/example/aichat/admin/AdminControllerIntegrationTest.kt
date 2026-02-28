package com.example.aichat.admin

import com.example.aichat.domain.ActivityLogEntity
import com.example.aichat.domain.ActivityType
import com.example.aichat.domain.ChatEntity
import com.example.aichat.domain.ThreadEntity
import com.example.aichat.domain.UserEntity
import com.example.aichat.domain.UserRole
import com.example.aichat.repository.ActivityLogRepository
import com.example.aichat.repository.ChatRepository
import com.example.aichat.repository.FeedbackRepository
import com.example.aichat.repository.ThreadRepository
import com.example.aichat.repository.UserRepository
import com.example.aichat.security.JwtService
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val threadRepository: ThreadRepository,
    @Autowired private val chatRepository: ChatRepository,
    @Autowired private val feedbackRepository: FeedbackRepository,
    @Autowired private val activityLogRepository: ActivityLogRepository,
    @Autowired private val jwtService: JwtService,
) {
    @BeforeEach
    fun clearData() {
        feedbackRepository.deleteAll()
        chatRepository.deleteAll()
        threadRepository.deleteAll()
        activityLogRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `metrics endpoint requires admin role`() {
        val memberToken = jwtService.createAccessToken(UUID.randomUUID(), UserRole.MEMBER)
        val adminToken = jwtService.createAccessToken(UUID.randomUUID(), UserRole.ADMIN)

        mockMvc.perform(get("/api/admin/metrics/daily"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            get("/api/admin/metrics/daily")
                .header("Authorization", "Bearer $memberToken"),
        )
            .andExpect(status().isForbidden)

        mockMvc.perform(
            get("/api/admin/metrics/daily")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `daily metrics returns signup login and chat create counts for last twenty four hours`() {
        val user = userRepository.save(
            UserEntity(
                email = "metrics-user@example.com",
                passwordHash = "pw",
                name = "Metrics User",
                role = UserRole.MEMBER,
            ),
        )

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        activityLogRepository.save(ActivityLogEntity(user = user, type = ActivityType.SIGNUP, createdAt = now.minusHours(1)))
        activityLogRepository.save(ActivityLogEntity(user = user, type = ActivityType.SIGNUP, createdAt = now.minusHours(2)))
        activityLogRepository.save(ActivityLogEntity(user = user, type = ActivityType.LOGIN, createdAt = now.minusMinutes(20)))
        activityLogRepository.save(ActivityLogEntity(user = user, type = ActivityType.CHAT_CREATE, createdAt = now.minusHours(3)))
        activityLogRepository.save(ActivityLogEntity(user = user, type = ActivityType.CHAT_CREATE, createdAt = now.minusHours(25)))

        val adminToken = jwtService.createAccessToken(UUID.randomUUID(), UserRole.ADMIN)
        val result = mockMvc.perform(
            get("/api/admin/metrics/daily")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        assertThat(body["signupCount"].asLong()).isEqualTo(2L)
        assertThat(body["loginCount"].asLong()).isEqualTo(1L)
        assertThat(body["chatCreateCount"].asLong()).isEqualTo(1L)
    }

    @Test
    fun `daily chats report returns csv with escaped values and user info`() {
        val owner = userRepository.save(
            UserEntity(
                email = "csv-owner@example.com",
                passwordHash = "pw",
                name = "CSV Owner",
                role = UserRole.MEMBER,
            ),
        )
        val thread = threadRepository.save(ThreadEntity(user = owner))
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val recentChat = chatRepository.save(
            ChatEntity(
                thread = thread,
                question = "hello, \"admin\"",
                answer = "line1\nline2",
                createdAt = now.minusMinutes(30),
            ),
        )
        val oldChat = chatRepository.save(
            ChatEntity(
                thread = thread,
                question = "old question",
                answer = "old answer",
                createdAt = now.minusHours(30),
            ),
        )

        val adminToken = jwtService.createAccessToken(UUID.randomUUID(), UserRole.ADMIN)
        val result = mockMvc.perform(
            get("/api/admin/reports/daily-chats.csv")
                .header("Authorization", "Bearer $adminToken"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType("text", "csv")))
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("charset=UTF-8")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"daily-chats-")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv\"")))
            .andReturn()

        val csvBytes = result.response.contentAsByteArray
        assertThat(csvBytes.size).isGreaterThanOrEqualTo(3)
        assertThat(csvBytes[0]).isEqualTo(0xEF.toByte())
        assertThat(csvBytes[1]).isEqualTo(0xBB.toByte())
        assertThat(csvBytes[2]).isEqualTo(0xBF.toByte())

        val csv = String(csvBytes, StandardCharsets.UTF_8).removePrefix("\uFEFF")
        assertThat(csv).contains("createdAt,userId,email,name,role,threadId,chatId,question,answer")
        assertThat(csv).contains(owner.email)
        assertThat(csv).contains(owner.name)
        assertThat(csv).contains(owner.role.name)

        assertThat(csv).contains(recentChat.id.toString())
        assertThat(csv).doesNotContain(oldChat.id.toString())

        assertThat(csv).contains("\"hello, \"\"admin\"\"\"")
        assertThat(csv).contains("\"line1\nline2\"")
    }
}
