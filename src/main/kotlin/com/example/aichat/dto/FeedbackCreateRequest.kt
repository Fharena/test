package com.example.aichat.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class FeedbackCreateRequest(
    @field:NotNull
    val chatId: UUID,

    @field:NotNull
    val isPositive: Boolean,
)
