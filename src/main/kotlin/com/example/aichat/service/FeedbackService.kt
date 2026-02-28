package com.example.aichat.service

import com.example.aichat.domain.FeedbackEntity
import com.example.aichat.domain.FeedbackStatus
import com.example.aichat.domain.UserRole
import com.example.aichat.dto.FeedbackCreateRequest
import com.example.aichat.dto.FeedbackListResponse
import com.example.aichat.dto.FeedbackResponse
import com.example.aichat.dto.FeedbackStatusUpdateRequest
import com.example.aichat.repository.ChatRepository
import com.example.aichat.repository.FeedbackRepository
import com.example.aichat.repository.UserRepository
import com.example.aichat.security.AuthPrincipal
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createFeedback(principal: AuthPrincipal, request: FeedbackCreateRequest): FeedbackResponse {
        val user = userRepository.findById(principal.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found") }
        val chat = chatRepository.findById(request.chatId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found") }

        val chatOwnerId = chat.thread.user.id ?: throw IllegalStateException("Thread owner id is required")
        if (principal.role == UserRole.MEMBER && chatOwnerId != principal.userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create feedback for your own chat")
        }

        val chatId = chat.id ?: throw IllegalStateException("Chat id is required")
        val duplicate = feedbackRepository.existsByUserIdAndChatId(principal.userId, chatId)
        if (duplicate) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Feedback already exists for this chat")
        }

        val feedback = try {
            feedbackRepository.save(
                FeedbackEntity(
                    user = user,
                    chat = chat,
                    isPositive = request.isPositive,
                    status = FeedbackStatus.PENDING,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Feedback already exists for this chat")
        }

        return feedback.toResponse()
    }

    @Transactional(readOnly = true)
    fun getFeedbacks(
        principal: AuthPrincipal,
        pageable: Pageable,
        isPositive: Boolean?,
    ): FeedbackListResponse {
        val page = when (principal.role) {
            UserRole.ADMIN -> {
                if (isPositive == null) feedbackRepository.findAll(pageable)
                else feedbackRepository.findAllByIsPositive(isPositive, pageable)
            }

            UserRole.MEMBER -> {
                if (isPositive == null) feedbackRepository.findAllByUserId(principal.userId, pageable)
                else feedbackRepository.findAllByUserIdAndIsPositive(principal.userId, isPositive, pageable)
            }
        }

        return FeedbackListResponse(
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious(),
            sort = pageable.sort.toString(),
            items = page.content.map { it.toResponse() },
        )
    }

    @Transactional
    fun updateStatus(
        principal: AuthPrincipal,
        feedbackId: java.util.UUID,
        request: FeedbackStatusUpdateRequest,
    ): FeedbackResponse {
        if (principal.role != UserRole.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can update feedback status")
        }

        val feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found") }

        feedback.status = request.status
        return feedback.toResponse()
    }

    private fun FeedbackEntity.toResponse(): FeedbackResponse {
        val feedbackId = id ?: throw IllegalStateException("Feedback id is required")
        val userId = user.id ?: throw IllegalStateException("User id is required")
        val chatId = chat.id ?: throw IllegalStateException("Chat id is required")
        val threadId = chat.thread.id ?: throw IllegalStateException("Thread id is required")
        val created = createdAt ?: throw IllegalStateException("Feedback createdAt is required")

        return FeedbackResponse(
            id = feedbackId,
            userId = userId,
            chatId = chatId,
            threadId = threadId,
            isPositive = isPositive,
            status = status,
            createdAt = created,
        )
    }
}
