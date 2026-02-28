package com.example.aichat.domain

import com.example.aichat.repository.ActivityLogRepository
import com.example.aichat.repository.ChatRepository
import com.example.aichat.repository.FeedbackRepository
import com.example.aichat.repository.ThreadRepository
import com.example.aichat.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DataJpaTest
class DomainRepositorySmokeTest(
    @Autowired private val userRepository: UserRepository,
    @Autowired private val threadRepository: ThreadRepository,
    @Autowired private val chatRepository: ChatRepository,
    @Autowired private val feedbackRepository: FeedbackRepository,
    @Autowired private val activityLogRepository: ActivityLogRepository,
) {
    @Test
    fun `persists entities and supports required repository queries`() {
        val user = userRepository.save(
            UserEntity(
                email = "member@example.com",
                passwordHash = "hashed-password",
                name = "member",
                role = UserRole.MEMBER,
            ),
        )

        val thread = threadRepository.save(
            ThreadEntity(
                user = user,
            ),
        )

        val chat = chatRepository.save(
            ChatEntity(
                thread = thread,
                question = "hello?",
                answer = "hi!",
            ),
        )

        val feedback = feedbackRepository.save(
            FeedbackEntity(
                user = user,
                chat = chat,
                isPositive = true,
                status = FeedbackStatus.PENDING,
            ),
        )

        val activityLog = activityLogRepository.save(
            ActivityLogEntity(
                user = user,
                type = ActivityType.CHAT_CREATE,
            ),
        )

        assertThat(user.id).isNotNull
        assertThat(user.createdAt).isNotNull
        assertThat(thread.id).isNotNull
        assertThat(thread.createdAt).isNotNull
        assertThat(thread.lastChatAt).isNotNull
        assertThat(chat.id).isNotNull
        assertThat(chat.createdAt).isNotNull
        assertThat(feedback.id).isNotNull
        assertThat(feedback.createdAt).isNotNull
        assertThat(activityLog.id).isNotNull
        assertThat(activityLog.createdAt).isNotNull

        val latestThread = threadRepository.findTopByUserOrderByLastChatAtDesc(user)
        assertThat(latestThread?.id).isEqualTo(thread.id)

        val chats = chatRepository.findAllByThreadIdInOrderByCreatedAtAsc(listOf(thread.id!!))
        assertThat(chats).hasSize(1)
        assertThat(chats.first().id).isEqualTo(chat.id)

        val existsFeedback = feedbackRepository.existsByUserIdAndChatId(user.id!!, chat.id!!)
        assertThat(existsFeedback).isTrue()

        val feedbackPage = feedbackRepository.findAllByUserId(user.id!!, PageRequest.of(0, 10))
        assertThat(feedbackPage.totalElements).isEqualTo(1)

        val from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        val to = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
        val chatCreateCount = activityLogRepository.countByTypeAndCreatedAtBetween(ActivityType.CHAT_CREATE, from, to)
        assertThat(chatCreateCount).isEqualTo(1)
    }

    @Test
    fun `enforces unique constraint for user and chat in feedback`() {
        val user = userRepository.save(
            UserEntity(
                email = "unique@example.com",
                passwordHash = "hashed-password",
                name = "unique",
                role = UserRole.MEMBER,
            ),
        )
        val thread = threadRepository.save(ThreadEntity(user = user))
        val chat = chatRepository.save(ChatEntity(thread = thread, question = "q", answer = "a"))

        feedbackRepository.saveAndFlush(
            FeedbackEntity(
                user = user,
                chat = chat,
                isPositive = true,
                status = FeedbackStatus.PENDING,
            ),
        )

        assertThatThrownBy {
            feedbackRepository.saveAndFlush(
                FeedbackEntity(
                    user = user,
                    chat = chat,
                    isPositive = false,
                    status = FeedbackStatus.RESOLVED,
                ),
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
