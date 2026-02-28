package com.example.aichat.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `maps ai provider exception to bad gateway`() {
        val request = MockHttpServletRequest("GET", "/api/chats")
        val response = handler.handleAiProviderException(AiProviderException("upstream failed"), request)

        assertThat(response.statusCode.value()).isEqualTo(502)
        assertThat(response.body?.code).isEqualTo("AI_PROVIDER_ERROR")
        assertThat(response.body?.message).isEqualTo("upstream failed")
        assertThat(response.body?.path).isEqualTo("/api/chats")
    }
}
