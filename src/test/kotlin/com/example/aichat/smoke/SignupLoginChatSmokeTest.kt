package com.example.aichat.smoke

import com.example.aichat.repository.ActivityLogRepository
import com.example.aichat.repository.ChatRepository
import com.example.aichat.repository.FeedbackRepository
import com.example.aichat.repository.ThreadRepository
import com.example.aichat.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignupLoginChatSmokeTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val threadRepository: ThreadRepository,
    @Autowired private val chatRepository: ChatRepository,
    @Autowired private val feedbackRepository: FeedbackRepository,
    @Autowired private val activityLogRepository: ActivityLogRepository,
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
    fun `signup and login then create one chat successfully`() {
        val signupPayload = mapOf(
            "email" to "smoke-user@example.com",
            "password" to "Password123!",
            "name" to "Smoke User",
        )

        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupPayload)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("smoke-user@example.com"))

        val loginPayload = mapOf(
            "email" to "smoke-user@example.com",
            "password" to "Password123!",
        )

        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginPayload)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andReturn()

        val accessToken = objectMapper.readTree(loginResult.response.contentAsString)
            .get("accessToken")
            .asText()

        val chatPayload = mapOf(
            "question" to "What is MVP?",
            "isStreaming" to false,
        )

        mockMvc.perform(
            post("/api/chats")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatPayload)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.question").value("What is MVP?"))
            .andExpect(jsonPath("$.answer").value("MOCK_RESPONSE[what is mvp?]"))
            .andExpect(jsonPath("$.chatId").isNotEmpty)
            .andExpect(jsonPath("$.threadId").isNotEmpty)

        assertThat(userRepository.count()).isEqualTo(1L)
        assertThat(threadRepository.count()).isEqualTo(1L)
        assertThat(chatRepository.count()).isEqualTo(1L)
    }
}
