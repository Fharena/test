package com.example.aichat.dto

import java.time.OffsetDateTime
import java.util.UUID

data class ChatCreateResponse(
    val threadId: UUID,
    val chatId: UUID,
    val question: String,
    val answer: String,
    val createdAt: OffsetDateTime,
)
