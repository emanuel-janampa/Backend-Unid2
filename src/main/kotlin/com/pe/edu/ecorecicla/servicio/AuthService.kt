package com.pe.edu.ecorecicla.servicio

import com.pe.edu.ecorecicla.config.JwtService
import com.pe.edu.ecorecicla.repositorio.UsuarioRepository
import com.pe.edu.ecorecicla.seguridad.Usuario
import com.pe.edu.ecorecicla.web.dto.LoginResponse
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    @Transactional(readOnly = true)
    fun iniciarSesion(username: String, password: String): LoginResponse {
        val user: Usuario = usuarioRepository.findByUsuarioIgnoreCase(username.trim())
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas")
        if (!user.activo || !passwordEncoder.matches(password, user.passwordHash)) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas")
        }
        val privileges = user.rol.privilegios.map { it.nombre }.toSortedSet()
        return LoginResponse(
            token = jwtService.crearToken(user),
            usuario = user.usuario,
            nombre = user.nombreCompleto,
            rol = user.rol.nombre,
            privilegios = privileges,
            expiraEnSegundos = jwtService.expiracionEnSegundos()
        )
    }
}
