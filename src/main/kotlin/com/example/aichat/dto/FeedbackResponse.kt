package com.example.aichat.dto

import com.example.aichat.domain.FeedbackStatus
import java.time.OffsetDateTime
import java.util.UUID

data class FeedbackResponse(
    val id: UUID,
    val userId: UUID,
    val chatId: UUID,
    val threadId: UUID,
    val isPositive: Boolean,
    val status: FeedbackStatus,
    val createdAt: OffsetDateTime,
)

data class FeedbackListResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val sort: String,
    val items: List<FeedbackResponse>,
)
