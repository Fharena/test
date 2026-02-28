package com.example.aichat.service

import com.example.aichat.ai.AiProvider
import com.example.aichat.ai.Message
import com.example.aichat.domain.ActivityType
import com.example.aichat.domain.ChatEntity
import com.example.aichat.domain.ThreadEntity
import com.example.aichat.domain.UserEntity
import com.example.aichat.dto.ChatCreateRequest
import com.example.aichat.dto.ChatCreateResponse
import com.example.aichat.repository.ChatRepository
import com.example.aichat.repository.ThreadRepository
import com.example.aichat.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Service
class ChatService(
    private val userRepository: UserRepository,
    private val threadRepository: ThreadRepository,
    private val chatRepository: ChatRepository,
    private val aiProvider: AiProvider,
    private val activityLogService: ActivityLogService,
) {
    @Transactional
    fun createChat(userId: UUID, request: ChatCreateRequest): ChatCreateResponse {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val user = getUser(userId)
        val thread = resolveThread(user = user, now = now)
        val question = request.question.trim()
        val messages = buildMessages(thread = thread, currentQuestion = question)

        val answer = aiProvider.generate(messages = messages, model = request.model)
        val chat = persistChat(thread = thread, user = user, question = question, answer = answer, now = now)

        return ChatCreateResponse(
            threadId = thread.id ?: throw IllegalStateException("Thread id was not generated"),
            chatId = chat.id ?: throw IllegalStateException("Chat id was not generated"),
            question = chat.question,
            answer = chat.answer,
            createdAt = chat.createdAt ?: throw IllegalStateException("Chat createdAt was not generated"),
        )
    }

    fun createChatStream(userId: UUID, request: ChatCreateRequest): SseEmitter {
        val emitter = SseEmitter(0L)

        CompletableFuture.runAsync {
            try {
                val now = OffsetDateTime.now(ZoneOffset.UTC)
                val user = getUser(userId)
                val thread = resolveThread(user = user, now = now)
                val question = request.question.trim()
                val messages = buildMessages(thread = thread, currentQuestion = question)

                val chunks = aiProvider.stream(messages = messages, model = request.model)
                val answerBuilder = StringBuilder()
                chunks.forEach { chunk ->
                    answerBuilder.append(chunk)
                    emitter.send(SseEmitter.event().name("chunk").data(chunk))
                }

                persistChat(
                    thread = thread,
                    user = user,
                    question = question,
                    answer = answerBuilder.toString(),
                    now = now,
                )

                emitter.send(SseEmitter.event().name("done").data("[DONE]"))
                emitter.complete()
            } catch (ex: Exception) {
                emitter.completeWithError(ex)
            }
        }

        return emitter
    }

    @Transactional
    protected fun persistChat(
        thread: ThreadEntity,
        user: UserEntity,
        question: String,
        answer: String,
        now: OffsetDateTime,
    ): ChatEntity {
        thread.lastChatAt = now
        threadRepository.save(thread)

        val chat = chatRepository.save(
            ChatEntity(
                thread = thread,
                question = question,
                answer = answer,
            ),
        )

        activityLogService.log(type = ActivityType.CHAT_CREATE, user = user)
        return chat
    }

    private fun getUser(userId: UUID): UserEntity {
        return userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found") }
    }

    private fun resolveThread(user: UserEntity, now: OffsetDateTime): ThreadEntity {
        val latestThread = threadRepository.findTopByUserOrderByLastChatAtDesc(user)
        if (latestThread == null) {
            return threadRepository.save(ThreadEntity(user = user, lastChatAt = now, createdAt = now))
        }

        val cutoff = now.minusMinutes(30)
        if (latestThread.lastChatAt?.isBefore(cutoff) == true) {
            return threadRepository.save(ThreadEntity(user = user, lastChatAt = now, createdAt = now))
        }

        return latestThread
    }

    private fun buildMessages(thread: ThreadEntity, currentQuestion: String): List<Message> {
        val threadId = thread.id ?: throw IllegalStateException("Thread id is required")
        val history = chatRepository.findAllByThreadIdInOrderByCreatedAtAsc(listOf(threadId))

        val messages = mutableListOf(Message(role = "system", content = "You are a helpful assistant"))
        history.forEach { chat ->
            messages += Message(role = "user", content = chat.question)
            messages += Message(role = "assistant", content = chat.answer)
        }
        messages += Message(role = "user", content = currentQuestion)
        return messages
    }
}
