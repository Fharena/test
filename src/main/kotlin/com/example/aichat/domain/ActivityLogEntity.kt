package com.example.aichat.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "activity_logs")
class ActivityLogEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    var user: UserEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: ActivityType,

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

enum class ActivityType {
    SIGNUP,
    LOGIN,
    CHAT_CREATE,
}
