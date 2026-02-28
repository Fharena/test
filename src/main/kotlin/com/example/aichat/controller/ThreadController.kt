package com.example.aichat.controller

import com.example.aichat.dto.ThreadListResponse
import com.example.aichat.security.AuthPrincipal
import com.example.aichat.service.ThreadService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.data.domain.Sort
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus
import java.util.UUID

@RestController
@RequestMapping("/api/threads")
class ThreadController(
    private val threadService: ThreadService,
) {
    @GetMapping
    fun getThreads(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ThreadListResponse {
        return threadService.getThreads(principal = principal, pageable = pageable)
    }

    @DeleteMapping("/{threadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteThread(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @PathVariable threadId: UUID,
    ) {
        threadService.deleteThread(principal = principal, threadId = threadId)
    }
}
