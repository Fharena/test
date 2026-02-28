package com.example.aichat.ai

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MockAiProviderTest {
    private val mockAiProvider = MockAiProvider()

    @Test
    fun `generate is deterministic based on last user message`() {
        val messages = listOf(
            Message(role = "system", content = "You are an assistant."),
            Message(role = "user", content = "   Hello   World   "),
            Message(role = "assistant", content = "ignored"),
            Message(role = "user", content = "How ARE You?"),
        )

        val first = mockAiProvider.generate(messages, null)
        val second = mockAiProvider.generate(messages, "any-model")

        assertThat(first).isEqualTo(second)
        assertThat(first).isEqualTo("MOCK_RESPONSE[how are you?]")
    }

    @Test
    fun `stream returns 20-char chunks that reconstruct generate output`() {
        val messages = listOf(Message(role = "user", content = "This is a stream chunking test message"))
        val fullText = mockAiProvider.generate(messages, null)

        val chunks = mockAiProvider.stream(messages, null).toList()

        assertThat(chunks).isNotEmpty
        assertThat(chunks.all { it.length <= 20 }).isTrue()
        assertThat(chunks.joinToString("")).isEqualTo(fullText)
    }
}
