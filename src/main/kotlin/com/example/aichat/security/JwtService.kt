package com.example.aichat.security

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.aichat.domain.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

@Service
class JwtService(
    @Value("\${app.security.jwt.secret}") private val secret: String,
    @Value("\${app.security.jwt.issuer}") private val issuer: String,
    @Value("\${app.security.jwt.access-token-exp-minutes}") private val accessTokenExpMinutes: Long,
) {
    private val algorithm: Algorithm by lazy { Algorithm.HMAC256(secret) }

    fun createAccessToken(userId: UUID, role: UserRole): String {
        val now = Instant.now()
        val exp = now.plus(accessTokenExpMinutes, ChronoUnit.MINUTES)

        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.toString())
            .withClaim(ROLE_CLAIM, role.name)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(exp))
            .sign(algorithm)
    }

    fun validateAndGetPrincipal(token: String): AuthPrincipal? {
        val decoded = verify(token) ?: return null
        val userId = runCatching { UUID.fromString(decoded.subject) }.getOrNull() ?: return null
        val roleName = decoded.getClaim(ROLE_CLAIM).asString() ?: return null
        val role = runCatching { UserRole.valueOf(roleName) }.getOrNull() ?: return null
        return AuthPrincipal(userId = userId, role = role)
    }

    private fun verify(token: String): DecodedJWT? {
        return try {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token)
        } catch (_: JWTVerificationException) {
            null
        }
    }

    companion object {
        private const val ROLE_CLAIM = "role"
    }
}
