package com.example.aichat.security

import com.example.aichat.domain.UserRole
import java.util.UUID

data class AuthPrincipal(
    val userId: UUID,
    val role: UserRole,
)
