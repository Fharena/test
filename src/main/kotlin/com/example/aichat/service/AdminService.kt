package com.example.aichat.service

import com.example.aichat.domain.ActivityType
import com.example.aichat.dto.AdminDailyChatsCsvReport
import com.example.aichat.dto.AdminDailyMetricsResponse
import com.example.aichat.repository.ActivityLogRepository
import com.example.aichat.repository.ChatRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class AdminService(
    private val activityLogRepository: ActivityLogRepository,
    private val chatRepository: ChatRepository,
) {
    @Transactional(readOnly = true)
    fun getDailyMetrics(): AdminDailyMetricsResponse {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val from = now.minusHours(24)

        return AdminDailyMetricsResponse(
            signupCount = activityLogRepository.countByTypeAndCreatedAtBetween(ActivityType.SIGNUP, from, now),
            loginCount = activityLogRepository.countByTypeAndCreatedAtBetween(ActivityType.LOGIN, from, now),
            chatCreateCount = activityLogRepository.countByTypeAndCreatedAtBetween(ActivityType.CHAT_CREATE, from, now),
        )
    }

    @Transactional(readOnly = true)
    fun generateDailyChatsCsv(): AdminDailyChatsCsvReport {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val from = now.minusHours(24)
        val chats = chatRepository.findAllWithThreadAndUserByCreatedAtBetween(from, now)

        val csv = StringBuilder()
        csv.appendLine("createdAt,userId,email,name,role,threadId,chatId,question,answer")

        chats.forEach { chat ->
            val chatId = chat.id ?: throw IllegalStateException("Chat id is required")
            val createdAt = chat.createdAt ?: throw IllegalStateException("Chat createdAt is required")
            val thread = chat.thread
            val threadId = thread.id ?: throw IllegalStateException("Thread id is required")
            val user = thread.user
            val userId = user.id ?: throw IllegalStateException("User id is required")

            val values = listOf(
                createdAt.toString(),
                userId.toString(),
                user.email,
                user.name,
                user.role.name,
                threadId.toString(),
                chatId.toString(),
                chat.question,
                chat.answer,
            )

            csv.appendLine(values.joinToString(",") { escapeCsv(it) })
        }

        val filenameTime = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss'Z'"))
        val fileName = "daily-chats-$filenameTime.csv"

        return AdminDailyChatsCsvReport(
            fileName = fileName,
            content = csv.toString(),
        )
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        val shouldWrap = escaped.contains(',') || escaped.contains('\n') || escaped.contains('\r') || escaped.contains('"')
        return if (shouldWrap) "\"$escaped\"" else escaped
    }
}
