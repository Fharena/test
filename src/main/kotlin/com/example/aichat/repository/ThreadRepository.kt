package com.example.aichat.repository

import com.example.aichat.domain.ThreadEntity
import com.example.aichat.domain.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ThreadRepository : JpaRepository<ThreadEntity, UUID> {
    fun findTopByUserOrderByLastChatAtDesc(user: UserEntity): ThreadEntity?
}
