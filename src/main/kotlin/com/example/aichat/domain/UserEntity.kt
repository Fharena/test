package com.example.aichat.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(nullable = false, unique = true, length = 320)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.MEMBER,

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

enum class UserRole {
    MEMBER,
    ADMIN,
}
