package com.pe.edu.ecorecicla.web

import com.pe.edu.ecorecicla.servicio.PuntoService
import com.pe.edu.ecorecicla.web.dto.CrearPuntoRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.ResponseStatus

@RestController
@RequestMapping("/api/v1/puntos")
class PuntoController(private val puntoService: PuntoService) {
    @GetMapping
    @PreAuthorize("hasAuthority('PUNTO_LEER')")
    fun listar(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) zona: String?
    ) = puntoService.listar(page, size, zona)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PUNTO_GESTIONAR')")
    fun crear(@Valid @RequestBody request: CrearPuntoRequest) = puntoService.crear(request)
}
