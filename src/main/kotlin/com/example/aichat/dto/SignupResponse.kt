package com.example.aichat.dto

import com.example.aichat.domain.UserRole
import java.time.OffsetDateTime
import java.util.UUID

data class SignupResponse(
    val userId: UUID,
    val email: String,
    val name: String,
    val role: UserRole,
    val createdAt: OffsetDateTime,
)
