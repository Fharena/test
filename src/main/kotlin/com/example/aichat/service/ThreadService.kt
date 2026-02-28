package com.example.aichat.service

import com.example.aichat.domain.UserRole
import com.example.aichat.dto.ThreadChatItem
import com.example.aichat.dto.ThreadItem
import com.example.aichat.dto.ThreadListResponse
import com.example.aichat.repository.ChatRepository
import com.example.aichat.repository.FeedbackRepository
import com.example.aichat.repository.ThreadRepository
import com.example.aichat.security.AuthPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ThreadService(
    private val threadRepository: ThreadRepository,
    private val chatRepository: ChatRepository,
    private val feedbackRepository: FeedbackRepository,
) {
    @Transactional(readOnly = true)
    fun getThreads(principal: AuthPrincipal, pageable: Pageable): ThreadListResponse {
        val threadPage = if (principal.role == UserRole.ADMIN) {
            threadRepository.findAll(pageable)
        } else {
            threadRepository.findAllByUserId(principal.userId, pageable)
        }

        val threadIds = threadPage.content.mapNotNull { it.id }
        val chats = if (threadIds.isEmpty()) {
            emptyList()
        } else {
            chatRepository.findAllByThreadIdInOrderByCreatedAtAsc(threadIds)
        }

        val chatsByThreadId = chats.groupBy { chat ->
            chat.thread.id ?: throw IllegalStateException("Thread id is required")
        }

        val items = threadPage.content.map { thread ->
            val threadId = thread.id ?: throw IllegalStateException("Thread id is required")
            val userId = thread.user.id ?: throw IllegalStateException("User id is required")
            val threadChats = chatsByThreadId[threadId].orEmpty().map { chat ->
                ThreadChatItem(
                    chatId = chat.id ?: throw IllegalStateException("Chat id is required"),
                    question = chat.question,
                    answer = chat.answer,
                    createdAt = chat.createdAt ?: throw IllegalStateException("Chat createdAt is required"),
                )
            }

            ThreadItem(
                threadId = threadId,
                userId = userId,
                createdAt = thread.createdAt ?: throw IllegalStateException("Thread createdAt is required"),
                lastChatAt = thread.lastChatAt ?: throw IllegalStateException("Thread lastChatAt is required"),
                chats = threadChats,
            )
        }

        return ThreadListResponse(
            page = threadPage.number,
            size = threadPage.size,
            totalElements = threadPage.totalElements,
            totalPages = threadPage.totalPages,
            hasNext = threadPage.hasNext(),
            hasPrevious = threadPage.hasPrevious(),
            sort = pageable.sort.toString(),
            items = items,
        )
    }

    @Transactional
    fun deleteThread(principal: AuthPrincipal, threadId: UUID) {
        val thread = threadRepository.findById(threadId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found") }

        val ownerId = thread.user.id ?: throw IllegalStateException("User id is required")
        val isOwner = ownerId == principal.userId
        val isAdmin = principal.role == UserRole.ADMIN
        if (!isOwner && !isAdmin) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this thread")
        }

        val chats = chatRepository.findAllByThreadIdInOrderByCreatedAtAsc(listOf(threadId))
        val chatIds = chats.mapNotNull { it.id }
        if (chatIds.isNotEmpty()) {
            feedbackRepository.deleteAllByChatIdIn(chatIds)
        }

        chatRepository.deleteAllByThreadIdIn(listOf(threadId))
        threadRepository.delete(thread)
    }
}
