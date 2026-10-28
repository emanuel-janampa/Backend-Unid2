package com.pe.edu.ecorecicla.config

import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().authentication == null) {
            val rawToken = header.removePrefix("Bearer ").trim()
            try {
                val principal = jwtService.validarYLeer(rawToken)
                val authorities = principal.privilegios.map(::SimpleGrantedAuthority)
                val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (_: JwtException) {
                // A missing/invalid/expired token is handled by the configured 401 entry point.
                SecurityContextHolder.clearContext()
            } catch (_: IllegalArgumentException) {
                SecurityContextHolder.clearContext()
            } catch (_: IllegalStateException) {
                SecurityContextHolder.clearContext()
            }
        }
        filterChain.doFilter(request, response)
    }
}
