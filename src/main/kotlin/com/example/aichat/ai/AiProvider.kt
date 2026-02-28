package com.example.aichat.ai

interface AiProvider {
    fun generate(messages: List<Message>, model: String? = null): String

    fun stream(messages: List<Message>, model: String? = null): Sequence<String> = sequence {
        yield(generate(messages, model))
    }
}

data class Message(
    val role: String,
    val content: String,
)
