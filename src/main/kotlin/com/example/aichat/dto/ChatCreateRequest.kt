package com.example.aichat.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChatCreateRequest(
    @field:NotBlank
    @field:Size(max = 4000)
    val question: String,

    val isStreaming: Boolean? = false,

    @field:Size(max = 100)
    val model: String? = null,
)
