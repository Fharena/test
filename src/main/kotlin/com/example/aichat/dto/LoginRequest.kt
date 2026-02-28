package com.example.aichat.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 320)
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String,
)
