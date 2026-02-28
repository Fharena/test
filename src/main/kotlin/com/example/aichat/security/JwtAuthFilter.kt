package com.example.aichat.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getBearerToken()
        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            val principal = jwtService.validateAndGetPrincipal(token)
            if (principal != null) {
                val authority = SimpleGrantedAuthority("ROLE_${principal.role.name}")
                val authentication = UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    listOf(authority),
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.getBearerToken(): String? {
        val authorization = getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        if (!authorization.startsWith(BEARER_PREFIX)) return null
        return authorization.removePrefix(BEARER_PREFIX).trim().takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
