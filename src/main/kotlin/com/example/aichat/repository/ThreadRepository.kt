package com.example.aichat.repository

import com.example.aichat.domain.ThreadEntity
import com.example.aichat.domain.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ThreadRepository : JpaRepository<ThreadEntity, UUID> {
    fun findTopByUserOrderByLastChatAtDesc(user: UserEntity): ThreadEntity?
    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<ThreadEntity>
}
