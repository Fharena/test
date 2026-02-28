package com.example.aichat.dto

import java.time.OffsetDateTime
import java.util.UUID

data class ThreadListResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val sort: String,
    val items: List<ThreadItem>,
)

data class ThreadItem(
    val threadId: UUID,
    val userId: UUID,
    val createdAt: OffsetDateTime,
    val lastChatAt: OffsetDateTime,
    val chats: List<ThreadChatItem>,
)

data class ThreadChatItem(
    val chatId: UUID,
    val question: String,
    val answer: String,
    val createdAt: OffsetDateTime,
)
