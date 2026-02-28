package com.example.aichat.controller

import com.example.aichat.dto.ChatCreateRequest
import com.example.aichat.service.ChatService
import com.example.aichat.security.AuthPrincipal
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val chatService: ChatService,
) {
    @PostMapping(
        produces = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_EVENT_STREAM_VALUE,
        ],
    )
    fun createChat(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @Valid @RequestBody request: ChatCreateRequest,
    ): Any {
        return if (request.isStreaming == true) {
            chatService.createChatStream(userId = principal.userId, request = request)
        } else {
            chatService.createChat(userId = principal.userId, request = request)
        }
    }
}
