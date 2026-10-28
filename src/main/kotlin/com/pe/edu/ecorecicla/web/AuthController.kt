package com.pe.edu.ecorecicla.web

import com.pe.edu.ecorecicla.config.UsuarioAutenticado
import com.pe.edu.ecorecicla.servicio.AuthService
import com.pe.edu.ecorecicla.web.dto.LoginRequest
import com.pe.edu.ecorecicla.web.dto.UsuarioActualDto
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest) =
        authService.iniciarSesion(request.usuario, request.contrasena)

    @GetMapping("/me")
    fun actual(@AuthenticationPrincipal actor: UsuarioAutenticado) = UsuarioActualDto(
        id = actor.id,
        usuario = actor.usuario,
        nombre = actor.nombre,
        rol = actor.rol,
        privilegios = actor.privilegios
    )
}
