package com.example.aichat.security

import com.example.aichat.domain.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtServiceTest {
    private val jwtService = JwtService(
        secret = "test-secret-1234567890",
        issuer = "test-issuer",
        accessTokenExpMinutes = 60L,
    )

    @Test
    fun `creates token and resolves principal`() {
        val userId = UUID.randomUUID()
        val token = jwtService.createAccessToken(userId = userId, role = UserRole.ADMIN)

        val principal = jwtService.validateAndGetPrincipal(token)

        assertThat(principal).isNotNull
        assertThat(principal?.userId).isEqualTo(userId)
        assertThat(principal?.role).isEqualTo(UserRole.ADMIN)
    }

    @Test
    fun `returns null for malformed token`() {
        val principal = jwtService.validateAndGetPrincipal("not-a-jwt")
        assertThat(principal).isNull()
    }
}
