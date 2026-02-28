package com.example.aichat.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(
    name = "threads",
    indexes = [
        Index(name = "idx_thread_user_last_chat_at", columnList = "user_id,last_chat_at"),
    ],
)
class ThreadEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Column(nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(nullable = false)
    var lastChatAt: OffsetDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        if (id == null) {
            id = UUID.randomUUID()
        }

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        if (createdAt == null) {
            createdAt = now
        }
        if (lastChatAt == null) {
            lastChatAt = now
        }
    }
}
