package com.example.aichat.dto

data class AdminDailyMetricsResponse(
    val signupCount: Long,
    val loginCount: Long,
    val chatCreateCount: Long,
)
