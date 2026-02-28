package com.example.aichat.controller

import com.example.aichat.dto.FeedbackCreateRequest
import com.example.aichat.dto.FeedbackListResponse
import com.example.aichat.dto.FeedbackResponse
import com.example.aichat.dto.FeedbackStatusUpdateRequest
import com.example.aichat.security.AuthPrincipal
import com.example.aichat.service.FeedbackService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/feedbacks")
class FeedbackController(
    private val feedbackService: FeedbackService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createFeedback(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @Valid @RequestBody request: FeedbackCreateRequest,
    ): FeedbackResponse {
        return feedbackService.createFeedback(principal = principal, request = request)
    }

    @GetMapping
    fun getFeedbacks(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
        @RequestParam(required = false) isPositive: Boolean?,
    ): FeedbackListResponse {
        return feedbackService.getFeedbacks(
            principal = principal,
            pageable = pageable,
            isPositive = isPositive,
        )
    }

    @PatchMapping("/{id}/status")
    fun updateFeedbackStatus(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: FeedbackStatusUpdateRequest,
    ): FeedbackResponse {
        return feedbackService.updateStatus(
            principal = principal,
            feedbackId = id,
            request = request,
        )
    }
}
