package com.example.aichat.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(AiProviderException::class)
    fun handleAiProviderException(ex: AiProviderException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.BAD_GATEWAY.value(),
                    code = "AI_PROVIDER_ERROR",
                    message = ex.message ?: "AI provider error",
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ApiErrorResponse> {
        val status = ex.statusCode
        return ResponseEntity.status(status)
            .body(
                ApiErrorResponse(
                    status = status.value(),
                    code = status.toString(),
                    message = ex.reason ?: "Request failed",
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val message = ex.bindingResult.fieldErrors
            .firstOrNull()
            ?.let { "${it.field}: ${it.defaultMessage ?: "invalid value"}" }
            ?: "Validation failed"

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    code = "VALIDATION_ERROR",
                    message = message,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    code = "INTERNAL_SERVER_ERROR",
                    message = ex.message ?: "Unexpected error",
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )
    }
}

data class ApiErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val timestamp: OffsetDateTime,
)
