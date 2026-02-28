package com.example.aichat.repository

import com.example.aichat.domain.ActivityLogEntity
import com.example.aichat.domain.ActivityType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface ActivityLogRepository : JpaRepository<ActivityLogEntity, UUID> {
    fun countByTypeAndCreatedAtBetween(type: ActivityType, from: OffsetDateTime, to: OffsetDateTime): Long
    fun countByCreatedAtBetween(from: OffsetDateTime, to: OffsetDateTime): Long
}
