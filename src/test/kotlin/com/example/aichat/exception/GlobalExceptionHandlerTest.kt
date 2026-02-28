package com.example.aichat.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `maps ai provider exception to bad gateway`() {
        val response = handler.handleAiProviderException(AiProviderException("upstream failed"))

        assertThat(response.statusCode.value()).isEqualTo(502)
        assertThat(response.body?.code).isEqualTo("AI_PROVIDER_ERROR")
        assertThat(response.body?.message).isEqualTo("upstream failed")
    }
}
