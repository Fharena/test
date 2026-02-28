package com.example.aichat.repository

import com.example.aichat.domain.FeedbackEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FeedbackRepository : JpaRepository<FeedbackEntity, UUID> {
    fun existsByUserIdAndChatId(userId: UUID, chatId: UUID): Boolean
    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<FeedbackEntity>
    fun findAllByChatId(chatId: UUID, pageable: Pageable): Page<FeedbackEntity>
    fun deleteAllByChatIdIn(chatIds: Collection<UUID>): Long
}
