package com.example.aichat.service

import com.example.aichat.domain.ActivityLogEntity
import com.example.aichat.domain.ActivityType
import com.example.aichat.domain.UserEntity
import com.example.aichat.repository.ActivityLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActivityLogService(
    private val activityLogRepository: ActivityLogRepository,
) {
    @Transactional
    fun log(type: ActivityType, user: UserEntity? = null) {
        activityLogRepository.save(ActivityLogEntity(user = user, type = type))
    }
}
