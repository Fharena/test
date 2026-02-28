package com.example.aichat.repository

import com.example.aichat.domain.ChatEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface ChatRepository : JpaRepository<ChatEntity, UUID> {
    fun findAllByThreadIdInOrderByCreatedAtAsc(threadIds: Collection<UUID>): List<ChatEntity>
    @Query(
        """
        select c
        from ChatEntity c
        join fetch c.thread t
        join fetch t.user u
        where c.createdAt between :from and :to
        order by c.createdAt asc
        """,
    )
    fun findAllWithThreadAndUserByCreatedAtBetween(
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime,
    ): List<ChatEntity>
    fun deleteAllByThreadIdIn(threadIds: Collection<UUID>): Long
}
