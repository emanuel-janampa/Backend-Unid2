package com.pe.edu.ecorecicla.config

import com.pe.edu.ecorecicla.web.ApiErrorWriter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
class SeguridadConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val apiErrorWriter: ApiErrorWriter
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // The JWT filter is attached to Spring Security's chain only, not registered twice as a servlet filter.
    @Bean
    fun disableContainerRegistration(): FilterRegistrationBean<JwtAuthenticationFilter> =
        FilterRegistrationBean<JwtAuthenticationFilter>(jwtAuthenticationFilter).apply {
            isEnabled = false
        }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.exceptionHandling { exceptions ->
            exceptions.authenticationEntryPoint { request, response, _ ->
                apiErrorWriter.write(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "Token ausente, alterado o vencido",
                    request.requestURI
                )
            }
            exceptions.accessDeniedHandler { request, response, _ ->
                apiErrorWriter.write(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Forbidden",
                    "No tienes el privilegio necesario para esta operación",
                    request.requestURI
                )
            }
        }
        http.authorizeHttpRequests { auth ->
            // The login endpoint is the only public route; every other endpoint requires a verified token.
            auth.requestMatchers("/api/v1/auth/login").permitAll()
            auth.anyRequest().authenticated()
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
