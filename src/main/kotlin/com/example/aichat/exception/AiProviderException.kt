package com.example.aichat.exception

class AiProviderException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
