package com.example.aichat.feedback

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackControllerIntegrationTest(
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
    fun `member can create feedback for own chat`() {
        val member = createUser("member-own@example.com", UserRole.MEMBER)
        val chat = createChat(owner = member, question = "q1", answer = "a1")
        val token = createToken(member.id!!, UserRole.MEMBER)

        val payload = mapOf(
            "chatId" to chat.id.toString(),
            "isPositive" to true,
        )

        mockMvc.perform(
            post("/api/feedbacks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.chatId").value(chat.id.toString()))
            .andExpect(jsonPath("$.userId").value(member.id.toString()))
            .andExpect(jsonPath("$.isPositive").value(true))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    fun `member cannot create feedback for others chat`() {
        val owner = createUser("owner@example.com", UserRole.MEMBER)
        val other = createUser("other@example.com", UserRole.MEMBER)
        val chat = createChat(owner = owner, question = "q1", answer = "a1")
        val token = createToken(other.id!!, UserRole.MEMBER)

        val payload = mapOf(
            "chatId" to chat.id.toString(),
            "isPositive" to false,
        )

        mockMvc.perform(
            post("/api/feedbacks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `duplicate feedback by same user and chat returns conflict`() {
        val member = createUser("dup-user@example.com", UserRole.MEMBER)
        val chat = createChat(owner = member, question = "q1", answer = "a1")
        val token = createToken(member.id!!, UserRole.MEMBER)
        val payload = mapOf(
            "chatId" to chat.id.toString(),
            "isPositive" to true,
        )

        mockMvc.perform(
            post("/api/feedbacks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/feedbacks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `admin can create feedback for any chat`() {
        val owner = createUser("owner-admin-target@example.com", UserRole.MEMBER)
        val admin = createUser("admin-feedback@example.com", UserRole.ADMIN)
        val chat = createChat(owner = owner, question = "q1", answer = "a1")
        val token = createToken(admin.id!!, UserRole.ADMIN)

        val payload = mapOf(
            "chatId" to chat.id.toString(),
            "isPositive" to true,
        )

        mockMvc.perform(
            post("/api/feedbacks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(admin.id.toString()))
    }

    @Test
    fun `member gets only own feedbacks and can filter by isPositive`() {
        val member1 = createUser("member-filter-1@example.com", UserRole.MEMBER)
        val member2 = createUser("member-filter-2@example.com", UserRole.MEMBER)
        val owner = createUser("owner-filter@example.com", UserRole.MEMBER)

        val chat1 = createChat(owner = owner, question = "q1", answer = "a1")
        val chat2 = createChat(owner = owner, question = "q2", answer = "a2")
        val chat3 = createChat(owner = owner, question = "q3", answer = "a3")

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        feedbackRepository.save(
            FeedbackEntity(
                user = member1,
                chat = chat1,
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = now.minusMinutes(3),
            ),
        )
        feedbackRepository.save(
            FeedbackEntity(
                user = member1,
                chat = chat2,
                isPositive = false,
                status = FeedbackStatus.PENDING,
                createdAt = now.minusMinutes(2),
            ),
        )
        feedbackRepository.save(
            FeedbackEntity(
                user = member2,
                chat = chat3,
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = now.minusMinutes(1),
            ),
        )

        val token = createToken(member1.id!!, UserRole.MEMBER)

        val allResult = mockMvc.perform(
            get("/api/feedbacks")
                .header("Authorization", "Bearer $token")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,asc"),
        )
            .andExpect(status().isOk)
            .andReturn()
        val allBody = objectMapper.readTree(allResult.response.contentAsString)
        assertThat(allBody["totalElements"].asLong()).isEqualTo(2L)
        assertThat(allBody["items"].size()).isEqualTo(2)
        assertThat(allBody["items"][0]["isPositive"].asBoolean()).isTrue()
        assertThat(allBody["items"][1]["isPositive"].asBoolean()).isFalse()

        val filtered = mockMvc.perform(
            get("/api/feedbacks")
                .header("Authorization", "Bearer $token")
                .param("isPositive", "true"),
        )
            .andExpect(status().isOk)
            .andReturn()
        val filteredBody = objectMapper.readTree(filtered.response.contentAsString)
        assertThat(filteredBody["totalElements"].asLong()).isEqualTo(1L)
        assertThat(filteredBody["items"][0]["isPositive"].asBoolean()).isTrue()
    }

    @Test
    fun `admin gets all feedbacks and can filter by isPositive`() {
        val admin = createUser("admin-list@example.com", UserRole.ADMIN)
        val userA = createUser("user-a@example.com", UserRole.MEMBER)
        val userB = createUser("user-b@example.com", UserRole.MEMBER)
        val owner = createUser("owner-admin-list@example.com", UserRole.MEMBER)

        val chat1 = createChat(owner = owner, question = "q1", answer = "a1")
        val chat2 = createChat(owner = owner, question = "q2", answer = "a2")

        feedbackRepository.save(FeedbackEntity(user = userA, chat = chat1, isPositive = true, status = FeedbackStatus.PENDING))
        feedbackRepository.save(FeedbackEntity(user = userB, chat = chat2, isPositive = false, status = FeedbackStatus.PENDING))

        val token = createToken(admin.id!!, UserRole.ADMIN)

        val all = mockMvc.perform(
            get("/api/feedbacks")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andReturn()
        val allBody = objectMapper.readTree(all.response.contentAsString)
        assertThat(allBody["totalElements"].asLong()).isEqualTo(2L)

        val filtered = mockMvc.perform(
            get("/api/feedbacks")
                .header("Authorization", "Bearer $token")
                .param("isPositive", "false"),
        )
            .andExpect(status().isOk)
            .andReturn()
        val filteredBody = objectMapper.readTree(filtered.response.contentAsString)
        assertThat(filteredBody["totalElements"].asLong()).isEqualTo(1L)
        assertThat(filteredBody["items"][0]["isPositive"].asBoolean()).isFalse()
    }

    @Test
    fun `patch status is admin only`() {
        val admin = createUser("admin-patch@example.com", UserRole.ADMIN)
        val member = createUser("member-patch@example.com", UserRole.MEMBER)
        val owner = createUser("owner-patch@example.com", UserRole.MEMBER)
        val chat = createChat(owner = owner, question = "q1", answer = "a1")
        val feedback = feedbackRepository.save(
            FeedbackEntity(
                user = member,
                chat = chat,
                isPositive = true,
                status = FeedbackStatus.PENDING,
            ),
        )

        val memberToken = createToken(member.id!!, UserRole.MEMBER)
        val adminToken = createToken(admin.id!!, UserRole.ADMIN)
        val payload = mapOf("status" to "RESOLVED")

        mockMvc.perform(
            patch("/api/feedbacks/${feedback.id}/status")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            patch("/api/feedbacks/${feedback.id}/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESOLVED"))

        val updated = feedbackRepository.findById(feedback.id!!).orElseThrow()
        assertThat(updated.status).isEqualTo(FeedbackStatus.RESOLVED)
    }

    private fun createUser(email: String, role: UserRole): UserEntity {
        return userRepository.save(
            UserEntity(
                email = email,
                passwordHash = "pw",
                name = email.substringBefore("@"),
                role = role,
            ),
        )
    }

    private fun createChat(owner: UserEntity, question: String, answer: String): ChatEntity {
        val thread = threadRepository.save(ThreadEntity(user = owner))
        return chatRepository.save(ChatEntity(thread = thread, question = question, answer = answer))
    }

    private fun createToken(userId: UUID, role: UserRole): String = jwtService.createAccessToken(userId, role)
}
