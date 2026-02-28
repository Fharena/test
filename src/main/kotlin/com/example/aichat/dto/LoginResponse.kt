package com.example.aichat.dto

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
)
