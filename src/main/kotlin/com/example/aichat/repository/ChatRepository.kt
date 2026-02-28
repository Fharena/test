package com.example.aichat.repository

import com.example.aichat.domain.ChatEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChatRepository : JpaRepository<ChatEntity, UUID> {
    fun findAllByThreadIdInOrderByCreatedAtAsc(threadIds: Collection<UUID>): List<ChatEntity>
}
