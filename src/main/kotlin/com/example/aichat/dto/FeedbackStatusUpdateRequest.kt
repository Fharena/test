package com.example.aichat.dto

import com.example.aichat.domain.FeedbackStatus
import jakarta.validation.constraints.NotNull

data class FeedbackStatusUpdateRequest(
    @field:NotNull
    val status: FeedbackStatus,
)
