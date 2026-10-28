package com.pe.edu.ecorecicla.web

import com.pe.edu.ecorecicla.config.UsuarioAutenticado
import com.pe.edu.ecorecicla.servicio.PesajeService
import com.pe.edu.ecorecicla.web.dto.AnularPesajeRequest
import com.pe.edu.ecorecicla.web.dto.RegistrarPesajeRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class PesajeController(private val pesajeService: PesajeService) {
    @GetMapping("/puntos/{puntoId}/pesajes")
    @PreAuthorize("hasAuthority('PESAJE_LEER')")
    fun listarPorPunto(
        @PathVariable puntoId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = pesajeService.listarPorPunto(puntoId, page, size)

    @PostMapping("/puntos/{puntoId}/pesajes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PESAJE_CREAR')")
    fun registrar(
        @PathVariable puntoId: Long,
        @Valid @RequestBody request: RegistrarPesajeRequest,
        @AuthenticationPrincipal actor: UsuarioAutenticado
    ) = pesajeService.registrar(puntoId, request, actor)

    @PutMapping("/pesajes/{pesajeId}")
    @PreAuthorize("hasAuthority('PESAJE_EDITAR')")
    fun corregir(
        @PathVariable pesajeId: Long,
        @Valid @RequestBody request: RegistrarPesajeRequest,
        @AuthenticationPrincipal actor: UsuarioAutenticado
    ) = pesajeService.corregir(pesajeId, request, actor)

    @PatchMapping("/pesajes/{pesajeId}/anular")
    @PreAuthorize("hasAuthority('PESAJE_ANULAR')")
    fun anular(
        @PathVariable pesajeId: Long,
        @Valid @RequestBody request: AnularPesajeRequest
    ) = pesajeService.anular(pesajeId, request)
}
