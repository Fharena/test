package com.example.aichat.service

import com.example.aichat.domain.ActivityType
import com.example.aichat.domain.UserEntity
import com.example.aichat.domain.UserRole
import com.example.aichat.dto.LoginRequest
import com.example.aichat.dto.LoginResponse
import com.example.aichat.dto.SignupRequest
import com.example.aichat.dto.SignupResponse
import com.example.aichat.repository.UserRepository
import com.example.aichat.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val activityLogService: ActivityLogService,
) {
    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        val normalizedEmail = normalizeEmail(request.email)
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already exists")
        }

        val user = userRepository.save(
            UserEntity(
                email = normalizedEmail,
                passwordHash = passwordEncoder.encode(request.password),
                name = request.name.trim(),
                role = UserRole.MEMBER,
            ),
        )

        activityLogService.log(type = ActivityType.SIGNUP, user = user)

        return SignupResponse(
            userId = user.id ?: throw IllegalStateException("User id was not generated"),
            email = user.email,
            name = user.name,
            role = user.role,
            createdAt = user.createdAt ?: throw IllegalStateException("User createdAt was not generated"),
        )
    }

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val normalizedEmail = normalizeEmail(request.email)
        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        }

        activityLogService.log(type = ActivityType.LOGIN, user = user)

        val token = jwtService.createAccessToken(
            userId = user.id ?: throw IllegalStateException("User id is required"),
            role = user.role,
        )
        return LoginResponse(accessToken = token)
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()
}
