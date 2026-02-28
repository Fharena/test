package com.example.aichat.thread

import com.example.aichat.domain.ChatEntity
import com.example.aichat.domain.FeedbackEntity
import com.example.aichat.domain.FeedbackStatus
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class ThreadControllerIntegrationTest(
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
    fun `member sees only own threads with chats grouped`() {
        val user1 = userRepository.save(
            UserEntity(
                email = "member1@example.com",
                passwordHash = "pw",
                name = "Member 1",
                role = UserRole.MEMBER,
            ),
        )
        val user2 = userRepository.save(
            UserEntity(
                email = "member2@example.com",
                passwordHash = "pw",
                name = "Member 2",
                role = UserRole.MEMBER,
            ),
        )

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val threadA = threadRepository.save(ThreadEntity(user = user1, createdAt = now.minusMinutes(20), lastChatAt = now.minusMinutes(5)))
        val threadB = threadRepository.save(ThreadEntity(user = user1, createdAt = now.minusMinutes(10), lastChatAt = now.minusMinutes(3)))
        val threadOther = threadRepository.save(ThreadEntity(user = user2, createdAt = now.minusMinutes(1), lastChatAt = now.minusMinutes(1)))

        chatRepository.save(ChatEntity(thread = threadA, question = "q1", answer = "a1", createdAt = now.minusMinutes(19)))
        chatRepository.save(ChatEntity(thread = threadA, question = "q2", answer = "a2", createdAt = now.minusMinutes(18)))
        chatRepository.save(ChatEntity(thread = threadB, question = "q3", answer = "a3", createdAt = now.minusMinutes(9)))
        chatRepository.save(ChatEntity(thread = threadOther, question = "q4", answer = "a4", createdAt = now.minusMinutes(1)))

        val token = createToken(user1.id!!, UserRole.MEMBER)

        val result = mockMvc.perform(
            get("/api/threads")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        assertThat(body["totalElements"].asLong()).isEqualTo(2L)
        assertThat(body["items"].size()).isEqualTo(2)

        val userIds = body["items"].map { it["userId"].asText() }.toSet()
        assertThat(userIds).containsExactly(user1.id.toString())

        val threadIds = body["items"].map { it["threadId"].asText() }
        assertThat(threadIds).containsExactly(threadB.id.toString(), threadA.id.toString())

        val threadAJson = body["items"].first { it["threadId"].asText() == threadA.id.toString() }
        val questions = threadAJson["chats"].map { it["question"].asText() }
        assertThat(questions).containsExactly("q1", "q2")
    }

    @Test
    fun `admin sees all threads`() {
        val member = userRepository.save(
            UserEntity(
                email = "member@example.com",
                passwordHash = "pw",
                name = "Member",
                role = UserRole.MEMBER,
            ),
        )
        val admin = userRepository.save(
            UserEntity(
                email = "admin@example.com",
                passwordHash = "pw",
                name = "Admin",
                role = UserRole.ADMIN,
            ),
        )

        threadRepository.save(ThreadEntity(user = member))
        threadRepository.save(ThreadEntity(user = admin))

        val token = createToken(admin.id!!, UserRole.ADMIN)

        val result = mockMvc.perform(
            get("/api/threads")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        assertThat(body["totalElements"].asLong()).isEqualTo(2L)
    }

    @Test
    fun `member can delete own thread and related chats and feedbacks`() {
        val owner = userRepository.save(
            UserEntity(
                email = "owner@example.com",
                passwordHash = "pw",
                name = "Owner",
                role = UserRole.MEMBER,
            ),
        )
        val thread = threadRepository.save(ThreadEntity(user = owner))
        val chat = chatRepository.save(ChatEntity(thread = thread, question = "q", answer = "a"))
        feedbackRepository.save(
            FeedbackEntity(
                user = owner,
                chat = chat,
                isPositive = true,
                status = FeedbackStatus.PENDING,
            ),
        )

        val token = createToken(owner.id!!, UserRole.MEMBER)

        mockMvc.perform(
            delete("/api/threads/${thread.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNoContent)

        assertThat(threadRepository.findById(thread.id!!)).isNotPresent
        assertThat(chatRepository.findAll()).isEmpty()
        assertThat(feedbackRepository.findAll()).isEmpty()
    }

    @Test
    fun `member cannot delete other users thread`() {
        val owner = userRepository.save(
            UserEntity(
                email = "owner2@example.com",
                passwordHash = "pw",
                name = "Owner2",
                role = UserRole.MEMBER,
            ),
        )
        val other = userRepository.save(
            UserEntity(
                email = "other@example.com",
                passwordHash = "pw",
                name = "Other",
                role = UserRole.MEMBER,
            ),
        )
        val thread = threadRepository.save(ThreadEntity(user = owner))
        val token = createToken(other.id!!, UserRole.MEMBER)

        mockMvc.perform(
            delete("/api/threads/${thread.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isForbidden)

        assertThat(threadRepository.findById(thread.id!!)).isPresent
    }

    @Test
    fun `admin can delete any thread`() {
        val owner = userRepository.save(
            UserEntity(
                email = "owner3@example.com",
                passwordHash = "pw",
                name = "Owner3",
                role = UserRole.MEMBER,
            ),
        )
        val thread = threadRepository.save(ThreadEntity(user = owner))
        val token = createToken(UUID.randomUUID(), UserRole.ADMIN)

        mockMvc.perform(
            delete("/api/threads/${thread.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNoContent)

        assertThat(threadRepository.findById(thread.id!!)).isNotPresent
    }

    private fun createToken(userId: UUID, role: UserRole): String = jwtService.createAccessToken(userId, role)
}
