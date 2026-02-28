package com.example.aichat.ai

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("dev", "test")
class MockAiProvider : AiProvider {
    override fun generate(messages: List<Message>, model: String?): String {
        val question = messages.lastOrNull { it.role.equals("user", ignoreCase = true) }?.content
            ?.trim()
            ?.ifBlank { null }
            ?: "empty-question"

        val normalized = question.lowercase().replace("\\s+".toRegex(), " ")
        return "MOCK_RESPONSE[$normalized]"
    }

    override fun stream(messages: List<Message>, model: String?): Sequence<String> {
        val fullText = generate(messages = messages, model = model)
        return fullText.chunked(STREAM_CHUNK_SIZE).asSequence()
    }

    companion object {
        private const val STREAM_CHUNK_SIZE = 20
    }
}
