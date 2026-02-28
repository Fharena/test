package com.example.aichat.exception

import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(AiProviderException::class)
    fun handleAiProviderException(
        ex: AiProviderException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        return buildResponse(
            status = HttpStatus.BAD_GATEWAY,
            code = "AI_PROVIDER_ERROR",
            message = ex.message ?: "AI provider error",
            request = request,
        )
    }

    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        ConstraintViolationException::class,
        HttpMessageNotReadableException::class,
    )
    fun handleValidationException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val message = when (ex) {
            is MethodArgumentNotValidException -> {
                ex.bindingResult.fieldErrors.firstOrNull()
                    ?.let { "${it.field}: ${it.defaultMessage ?: "invalid value"}" }
                    ?: "Validation failed"
            }
            is ConstraintViolationException -> ex.constraintViolations.firstOrNull()?.message ?: "Validation failed"
            else -> "Malformed request body"
        }

        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            code = "VALIDATION_ERROR",
            message = message,
            request = request,
        )
    }

    @ExceptionHandler(
        EntityNotFoundException::class,
        NoSuchElementException::class,
    )
    fun handleNotFoundException(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> {
        return buildResponse(
            status = HttpStatus.NOT_FOUND,
            code = "NOT_FOUND",
            message = ex.message ?: "Resource not found",
            request = request,
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleForbiddenException(
        ex: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        return buildResponse(
            status = HttpStatus.FORBIDDEN,
            code = "FORBIDDEN",
            message = ex.message ?: "Access denied",
            request = request,
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConflictException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        return buildResponse(
            status = HttpStatus.CONFLICT,
            code = "CONFLICT",
            message = ex.mostSpecificCause.message ?: "Resource conflict",
            request = request,
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        val code = when (status) {
            HttpStatus.BAD_REQUEST -> "VALIDATION_ERROR"
            HttpStatus.NOT_FOUND -> "NOT_FOUND"
            HttpStatus.FORBIDDEN -> "FORBIDDEN"
            HttpStatus.CONFLICT -> "CONFLICT"
            HttpStatus.BAD_GATEWAY -> "AI_PROVIDER_ERROR"
            else -> status.name
        }

        return buildResponse(
            status = status,
            code = code,
            message = ex.reason ?: "Request failed",
            request = request,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            code = "INTERNAL_SERVER_ERROR",
            message = ex.message ?: "Unexpected error",
            request = request,
        )
    }

    private fun buildResponse(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity.status(status)
            .body(
                ApiErrorResponse(
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                    status = status.value(),
                    code = code,
                    message = message,
                    path = request.requestURI ?: "",
                ),
            )
    }
}

data class ApiErrorResponse(
    val timestamp: OffsetDateTime,
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
)
