package com.example.aichat.ai

import com.example.aichat.exception.AiProviderException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
@Profile("prod")
class OpenAiProvider(
    webClientBuilder: WebClient.Builder,
    @Value("\${app.openai.base-url}") private val baseUrl: String,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.model:gpt-4o-mini}") private val defaultModel: String,
) : AiProvider {
    private val webClient: WebClient = webClientBuilder.baseUrl(baseUrl).build()

    override fun generate(messages: List<Message>, model: String?): String {
        if (apiKey.isBlank()) {
            throw AiProviderException("OpenAI API key is missing")
        }
        if (messages.isEmpty()) {
            throw AiProviderException("At least one message is required")
        }

        val request = OpenAiChatCompletionRequest(
            model = model ?: defaultModel,
            messages = messages.map { OpenAiMessage(role = it.role, content = it.content) },
        )

        val response = try {
            webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError) { clientResponse ->
                    clientResponse.bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .map { body ->
                            AiProviderException(
                                "OpenAI request failed with status ${clientResponse.statusCode().value()}: $body",
                            )
                        }
                }
                .bodyToMono(OpenAiChatCompletionResponse::class.java)
                .block()
        } catch (e: WebClientResponseException) {
            throw AiProviderException("OpenAI response error: ${e.statusCode.value()} ${e.responseBodyAsString}", e)
        } catch (e: Exception) {
            throw AiProviderException("OpenAI call failed", e)
        }

        return response?.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw AiProviderException("OpenAI returned an empty response")
    }

    override fun stream(messages: List<Message>, model: String?): Sequence<String> {
        return sequenceOf(generate(messages = messages, model = model))
    }
}

private data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
)

private data class OpenAiMessage(
    val role: String,
    val content: String,
)

private data class OpenAiChatCompletionResponse(
    val choices: List<OpenAiChoice> = emptyList(),
)

private data class OpenAiChoice(
    val message: OpenAiAssistantMessage,
)

private data class OpenAiAssistantMessage(
    val role: String? = null,
    val content: String? = null,
)
