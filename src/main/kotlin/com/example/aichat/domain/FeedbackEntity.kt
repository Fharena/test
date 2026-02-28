package com.example.aichat.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(
    name = "feedbacks",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_feedback_user_chat", columnNames = ["user_id", "chat_id"]),
    ],
    indexes = [
        Index(name = "idx_feedback_user_created_at", columnList = "user_id,created_at"),
    ],
)
class FeedbackEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    var chat: ChatEntity,

    @Column(nullable = false)
    var isPositive: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: FeedbackStatus = FeedbackStatus.PENDING,

    @Column(nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        if (id == null) {
            id = UUID.randomUUID()
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }
}

enum class FeedbackStatus {
    PENDING,
    RESOLVED,
}
