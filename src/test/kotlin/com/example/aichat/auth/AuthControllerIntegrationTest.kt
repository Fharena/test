package com.example.aichat.auth

import com.example.aichat.domain.ActivityType
import com.example.aichat.domain.UserEntity
import com.example.aichat.domain.UserRole
import com.example.aichat.repository.ActivityLogRepository
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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val activityLogRepository: ActivityLogRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val jwtService: JwtService,
) {
    @BeforeEach
    fun clearData() {
        activityLogRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `signup creates member user and writes signup activity log`() {
        val payload = mapOf(
            "email" to "new-user@example.com",
            "password" to "Password123!",
            "name" to "New User",
        )

        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("new-user@example.com"))
            .andExpect(jsonPath("$.name").value("New User"))
            .andExpect(jsonPath("$.role").value("MEMBER"))

        val savedUser = userRepository.findByEmail("new-user@example.com")
        assertThat(savedUser).isNotNull
        assertThat(savedUser!!.role).isEqualTo(UserRole.MEMBER)
        assertThat(savedUser.passwordHash).isNotEqualTo("Password123!")
        assertThat(passwordEncoder.matches("Password123!", savedUser.passwordHash)).isTrue()

        val from = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5)
        val to = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5)
        val signupCount = activityLogRepository.countByTypeAndCreatedAtBetween(ActivityType.SIGNUP, from, to)
        assertThat(signupCount).isEqualTo(1)
    }

    @Test
    fun `signup returns conflict when email already exists`() {
        val payload = mapOf(
            "email" to "dup@example.com",
            "password" to "Password123!",
            "name" to "Dup User",
        )

        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        ).andExpect(status().isConflict)
    }

    @Test
    fun `login returns jwt when credentials are valid and logs login activity`() {
        userRepository.save(
            UserEntity(
                email = "member@example.com",
                passwordHash = passwordEncoder.encode("Password123!"),
                name = "Member",
                role = UserRole.MEMBER,
            ),
        )

        val payload = mapOf(
            "email" to "member@example.com",
            "password" to "Password123!",
        )

        val response = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andReturn()

        val body = objectMapper.readTree(response.response.contentAsString)
        val accessToken = body.get("accessToken").asText()
        val principal = jwtService.validateAndGetPrincipal(accessToken)
        assertThat(principal).isNotNull
        assertThat(principal?.role).isEqualTo(UserRole.MEMBER)

        val from = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5)
        val to = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5)
        val loginCount = activityLogRepository.countByTypeAndCreatedAtBetween(ActivityType.LOGIN, from, to)
        assertThat(loginCount).isEqualTo(1)
    }

    @Test
    fun `login fails with unauthorized for invalid credentials`() {
        userRepository.save(
            UserEntity(
                email = "member@example.com",
                passwordHash = passwordEncoder.encode("Password123!"),
                name = "Member",
                role = UserRole.MEMBER,
            ),
        )

        val payload = mapOf(
            "email" to "member@example.com",
            "password" to "WrongPass123!",
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        ).andExpect(status().isUnauthorized)
    }
}
