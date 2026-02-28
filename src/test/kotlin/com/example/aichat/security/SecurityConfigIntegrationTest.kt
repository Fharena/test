package com.example.aichat.security

import com.example.aichat.domain.UserRole
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigIntegrationTest.TestApi::class)
@TestPropertySource(
    properties = [
        "app.security.jwt.secret=test-secret-1234567890",
        "app.security.jwt.issuer=test-issuer",
        "app.security.jwt.access-token-exp-minutes=60",
    ],
)
class SecurityConfigIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val jwtService: JwtService,
) {
    @Test
    fun `signup and login endpoints are permitAll`() {
        mockMvc.perform(get("/api/auth/signup"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/auth/login"))
            .andExpect(status().isOk)
    }

    @Test
    fun `non auth endpoint requires jwt`() {
        mockMvc.perform(get("/api/member/ping"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `admin path requires admin role`() {
        val memberToken = jwtService.createAccessToken(UUID.randomUUID(), UserRole.MEMBER)
        val adminToken = jwtService.createAccessToken(UUID.randomUUID(), UserRole.ADMIN)

        mockMvc.perform(
            get("/api/admin/ping")
                .header("Authorization", "Bearer $memberToken"),
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            get("/api/admin/ping")
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)
    }

    @Test
    fun `feedback status patch is admin only`() {
        val memberToken = jwtService.createAccessToken(UUID.randomUUID(), UserRole.MEMBER)
        val adminToken = jwtService.createAccessToken(UUID.randomUUID(), UserRole.ADMIN)
        val feedbackId = UUID.randomUUID()

        mockMvc.perform(
            patch("/api/feedback/$feedbackId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $memberToken"),
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            patch("/api/feedback/$feedbackId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $adminToken"),
        ).andExpect(status().isOk)
    }

    @RestController
    @RequestMapping("/api")
    class TestApi {
        @GetMapping("/auth/signup")
        fun signup(): String = "ok"

        @GetMapping("/auth/login")
        fun login(): String = "ok"

        @GetMapping("/member/ping")
        fun memberPing(): String = "ok"

        @GetMapping("/admin/ping")
        fun adminPing(): String = "ok"

        @PatchMapping("/feedback/{id}/status")
        fun updateFeedbackStatus(@PathVariable id: UUID): String = id.toString()
    }
}
