package com.example.aichat.chat

import com.example.aichat.domain.ActivityType
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIntegrationTest(
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
    fun `creates chat, saves answer, updates thread and logs activity`() {
        val user = userRepository.save(
            UserEntity(
                email = "chat-user@example.com",
                passwordHash = "pw",
                name = "Chat User",
                role = UserRole.MEMBER,
            ),
        )
        val token = createToken(user.id!!)

        val requestBody = mapOf(
            "question" to "Hello Bot",
            "isStreaming" to false,
        )

        val result = mockMvc.perform(
            post("/api/chats")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $token")
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.question").value("Hello Bot"))
            .andExpect(jsonPath("$.answer").value("MOCK_RESPONSE[hello bot]"))
            .andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        val threadId = UUID.fromString(body.get("threadId").asText())
        val chatId = UUID.fromString(body.get("chatId").asText())

        assertThat(threadRepository.findById(threadId)).isPresent
        assertThat(chatRepository.findById(chatId)).isPresent

        val from = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5)
        val to = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5)
        val count = activityLogRepository.countByTypeAndCreatedAtBetween(ActivityType.CHAT_CREATE, from, to)
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `reuses the same thread within thirty minutes`() {
        val user = userRepository.save(
            UserEntity(
                email = "thread-user@example.com",
                passwordHash = "pw",
                name = "Thread User",
                role = UserRole.MEMBER,
            ),
        )
        val token = createToken(user.id!!)

        val firstBody = createChatAndReadBody(token, "first question")
        val secondBody = createChatAndReadBody(token, "second question")

        val firstThreadId = firstBody.get("threadId").asText()
        val secondThreadId = secondBody.get("threadId").asText()
        assertThat(secondThreadId).isEqualTo(firstThreadId)
    }

    @Test
    fun `creates new thread when last chat is older than thirty minutes`() {
        val user = userRepository.save(
            UserEntity(
                email = "old-thread-user@example.com",
                passwordHash = "pw",
                name = "Old Thread User",
                role = UserRole.MEMBER,
            ),
        )

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val oldThread = threadRepository.save(
            ThreadEntity(
                user = user,
                createdAt = now.minusHours(1),
                lastChatAt = now.minusMinutes(31),
            ),
        )

        val token = createToken(user.id!!)
        val newBody = createChatAndReadBody(token, "new question")
        val newThreadId = newBody.get("threadId").asText()

        assertThat(newThreadId).isNotEqualTo(oldThread.id.toString())
    }

    @Test
    fun `streams chunks over sse and persists final answer`() {
        val user = userRepository.save(
            UserEntity(
                email = "stream-user@example.com",
                passwordHash = "pw",
                name = "Stream User",
                role = UserRole.MEMBER,
            ),
        )
        val token = createToken(user.id!!)

        val requestBody = mapOf(
            "question" to "Streaming Question",
            "isStreaming" to true,
        )

        mockMvc.perform(
            post("/api/chats")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header("Authorization", "Bearer $token")
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(request().asyncStarted())
            .andReturn()

        var savedAnswers = chatRepository.findAll().map { it.answer }
        repeat(20) {
            if (savedAnswers.contains("MOCK_RESPONSE[streaming question]")) {
                return@repeat
            }
            Thread.sleep(100)
            savedAnswers = chatRepository.findAll().map { it.answer }
        }

        assertThat(savedAnswers).contains("MOCK_RESPONSE[streaming question]")
    }

    private fun createChatAndReadBody(token: String, question: String) = run {
        val payload = mapOf(
            "question" to question,
        )

        val result = mockMvc.perform(
            post("/api/chats")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $token")
                .content(objectMapper.writeValueAsString(payload)),
        )
            .andExpect(status().isOk)
            .andReturn()

        objectMapper.readTree(result.response.contentAsString)
    }

    private fun createToken(userId: UUID): String = jwtService.createAccessToken(userId, UserRole.MEMBER)
}
