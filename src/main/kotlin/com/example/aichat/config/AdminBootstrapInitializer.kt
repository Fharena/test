package com.example.aichat.config

import com.example.aichat.domain.UserEntity
import com.example.aichat.domain.UserRole
import com.example.aichat.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminBootstrapInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.bootstrap-admin.enabled:false}") private val enabled: Boolean,
    @Value("\${app.bootstrap-admin.email:}") private val email: String,
    @Value("\${app.bootstrap-admin.password:}") private val password: String,
    @Value("\${app.bootstrap-admin.name:Admin}") private val name: String,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled) {
            return
        }

        val normalizedEmail = email.trim().lowercase()
        val rawPassword = password.trim()
        val displayName = name.trim()
        if (normalizedEmail.isBlank() || rawPassword.isBlank() || displayName.isBlank()) {
            logger.warn("Admin bootstrap is enabled, but email/password/name is missing. Skipping admin seed.")
            return
        }

        val existing = userRepository.findByEmail(normalizedEmail)
        if (existing != null) {
            if (existing.role == UserRole.ADMIN) {
                logger.info("Admin bootstrap account already exists: {}", normalizedEmail)
            } else {
                logger.warn(
                    "Admin bootstrap email already exists as non-admin user: {}. Skipping admin seed.",
                    normalizedEmail,
                )
            }
            return
        }

        userRepository.save(
            UserEntity(
                email = normalizedEmail,
                passwordHash = passwordEncoder.encode(rawPassword),
                name = displayName,
                role = UserRole.ADMIN,
            ),
        )

        logger.info("Admin bootstrap account created: {}", normalizedEmail)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AdminBootstrapInitializer::class.java)
    }
}
