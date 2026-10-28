package com.pe.edu.ecorecicla.web

import com.pe.edu.ecorecicla.config.UsuarioAutenticado
import com.pe.edu.ecorecicla.servicio.RutaService
import com.pe.edu.ecorecicla.web.dto.CrearRutaRequest
import com.pe.edu.ecorecicla.web.dto.OmitirPuntoRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/rutas")
class RutaController(private val rutaService: RutaService) {
    @GetMapping("/dia")
    @PreAuthorize("hasAuthority('PUNTO_LEER')")
    fun dia(
        @RequestParam(required = false) fecha: LocalDate?,
        @AuthenticationPrincipal actor: UsuarioAutenticado
    ) = rutaService.rutaDelDia(fecha ?: LocalDate.now(), actor)

    @GetMapping("/{rutaId}")
    @PreAuthorize("hasAuthority('PUNTO_LEER')")
    fun obtener(@PathVariable rutaId: Long, @AuthenticationPrincipal actor: UsuarioAutenticado) = rutaService.obtener(rutaId, actor)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PUNTO_GESTIONAR')")
    fun crear(@Valid @RequestBody request: CrearRutaRequest) = rutaService.crear(request)

    @PutMapping("/{rutaId}/puntos/{puntoId}/omision")
    @PreAuthorize("hasAuthority('RUTA_CERRAR')")
    fun omitir(
        @PathVariable rutaId: Long,
        @PathVariable puntoId: Long,
        @Valid @RequestBody request: OmitirPuntoRequest
    ) = rutaService.omitirPunto(rutaId, puntoId, request)

    @PostMapping("/{rutaId}/cerrar")
    @PreAuthorize("hasAuthority('RUTA_CERRAR')")
    fun cerrar(@PathVariable rutaId: Long) = rutaService.cerrar(rutaId)
}
