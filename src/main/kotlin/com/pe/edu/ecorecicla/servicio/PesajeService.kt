package com.pe.edu.ecorecicla.servicio

import com.pe.edu.ecorecicla.config.UsuarioAutenticado
import com.pe.edu.ecorecicla.dominio.EstadoRuta
import com.pe.edu.ecorecicla.dominio.Pesaje
import com.pe.edu.ecorecicla.dominio.RutaPunto
import com.pe.edu.ecorecicla.repositorio.PesajeRepository
import com.pe.edu.ecorecicla.repositorio.PuntoAcopioRepository
import com.pe.edu.ecorecicla.repositorio.RutaPuntoRepository
import com.pe.edu.ecorecicla.repositorio.UsuarioRepository
import com.pe.edu.ecorecicla.web.dto.AnularPesajeRequest
import com.pe.edu.ecorecicla.web.dto.PaginaRespuesta
import com.pe.edu.ecorecicla.web.dto.PesajeDto
import com.pe.edu.ecorecicla.web.dto.RegistrarPesajeRequest
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PesajeService(
    private val pesajeRepository: PesajeRepository,
    private val rutaPuntoRepository: RutaPuntoRepository,
    private val puntoRepository: PuntoAcopioRepository,
    private val usuarioRepository: UsuarioRepository
) {
    @Transactional(readOnly = true)
    fun listarPorPunto(puntoId: Long, page: Int, size: Int): PaginaRespuesta<PesajeDto> {
        if (!puntoRepository.existsById(puntoId)) throw ApiException(HttpStatus.NOT_FOUND, "No existe el punto de acopio $puntoId")
        validarPaginacion(page, size)
        return pesajeRepository.findByPuntoOrdenado(puntoId, PageRequest.of(page, size)).toRespuesta { it.toDto() }
    }

    @Transactional
    fun registrar(puntoId: Long, request: RegistrarPesajeRequest, actor: UsuarioAutenticado): PesajeDto {
        val rutaPunto = buscarPuntoEnRutaAbiertaHoy(puntoId, actor)
        val autor = usuarioRepository.findByUsuarioIgnoreCase(actor.usuario)
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "El usuario del token ya no existe")
        val (bruto, tara, neto) = calcularPesos(request.pesoBruto, request.tara)
        val pesaje = Pesaje(
            rutaPunto = rutaPunto,
            fechaHora = LocalDateTime.now(),
            material = request.material.trim(),
            pesoBruto = bruto,
            tara = tara,
            pesoNeto = neto,
            observacion = request.observacion?.trim()?.takeIf { it.isNotEmpty() },
            registradoPor = autor
        )
        return pesajeRepository.save(pesaje).toDto()
    }

    @Transactional
    fun corregir(pesajeId: Long, request: RegistrarPesajeRequest, actor: UsuarioAutenticado): PesajeDto {
        val pesaje = obtenerPesaje(pesajeId)
        if (pesaje.rutaPunto.ruta.estado == EstadoRuta.CERRADA) {
            throw ApiException(HttpStatus.CONFLICT, "No se puede corregir un pesaje de una ruta cerrada")
        }
        if (pesaje.anulado) {
            throw ApiException(HttpStatus.CONFLICT, "No se puede corregir un pesaje anulado; registra uno nuevo si corresponde")
        }
        if (!actor.puede("RUTA_CERRAR")) {
            if (!pesaje.registradoPor.usuario.equals(actor.usuario, ignoreCase = true)) {
                throw ApiException(HttpStatus.FORBIDDEN, "El operario solo puede corregir sus propios pesajes")
            }
            if (pesaje.fechaHora.toLocalDate() != LocalDate.now()) {
                throw ApiException(HttpStatus.FORBIDDEN, "El operario solo puede corregir pesajes del mismo día")
            }
        }
        val (bruto, tara, neto) = calcularPesos(request.pesoBruto, request.tara)
        pesaje.material = request.material.trim()
        pesaje.pesoBruto = bruto
        pesaje.tara = tara
        pesaje.pesoNeto = neto
        pesaje.observacion = request.observacion?.trim()?.takeIf { it.isNotEmpty() }
        return pesajeRepository.save(pesaje).toDto()
    }

    @Transactional
    fun anular(pesajeId: Long, request: AnularPesajeRequest): PesajeDto {
        val pesaje = obtenerPesaje(pesajeId)
        if (pesaje.anulado) throw ApiException(HttpStatus.CONFLICT, "El pesaje ya está anulado")
        // RN-01 specifically prevents registering/correcting on a closed route; annulment is a soft-delete.
        pesaje.anulado = true
        pesaje.motivoAnulacion = request.motivo.trim()
        return pesajeRepository.save(pesaje).toDto()
    }

    private fun buscarPuntoEnRutaAbiertaHoy(puntoId: Long, actor: UsuarioAutenticado): RutaPunto {
        val candidates = rutaPuntoRepository.findForPointAndDate(puntoId, LocalDate.now(), EstadoRuta.ABIERTA)
        val match = candidates.firstOrNull { it.ruta.operario.usuario.equals(actor.usuario, ignoreCase = true) }
            ?: if (actor.puede("RUTA_CERRAR")) candidates.firstOrNull() else null
        return match ?: throw ApiException(
            HttpStatus.CONFLICT,
            "El punto no pertenece a una ruta abierta de hoy asignada a este usuario"
        )
    }

    private fun obtenerPesaje(id: Long): Pesaje = pesajeRepository.findById(id)
        .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "No existe el pesaje $id") }

    private fun calcularPesos(pesoBrutoOriginal: BigDecimal, taraOriginal: BigDecimal): Triple<BigDecimal, BigDecimal, BigDecimal> {
        val bruto = pesoBrutoOriginal.setScale(3, RoundingMode.HALF_UP)
        val tara = taraOriginal.setScale(3, RoundingMode.HALF_UP)
        val neto = bruto.subtract(tara).setScale(3, RoundingMode.HALF_UP)
        if (neto <= BigDecimal.ZERO) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "El peso neto (peso bruto menos tara) debe ser mayor que cero",
                mapOf("pesoBruto" to "El peso bruto menos la tara debe ser mayor que cero")
            )
        }
        return Triple(bruto, tara, neto)
    }

    private fun validarPaginacion(page: Int, size: Int) {
        if (page < 0) throw ApiException(HttpStatus.BAD_REQUEST, "El parámetro page no puede ser negativo", mapOf("page" to "Debe ser 0 o mayor"))
        if (size !in 1..100) throw ApiException(HttpStatus.BAD_REQUEST, "El parámetro size debe estar entre 1 y 100", mapOf("size" to "Usa un tamaño de 1 a 100"))
    }
}

internal fun UsuarioAutenticado.puede(privilegio: String): Boolean = privilegios.contains(privilegio)

fun Pesaje.toDto() = PesajeDto(
    id = requireNotNull(id),
    puntoId = requireNotNull(rutaPunto.punto.id),
    rutaId = requireNotNull(rutaPunto.ruta.id),
    fechaHora = fechaHora.toString(),
    material = material,
    pesoBruto = pesoBruto,
    tara = tara,
    pesoNeto = pesoNeto,
    observacion = observacion,
    operario = registradoPor.usuario,
    anulado = anulado,
    motivoAnulacion = motivoAnulacion,
    pendiente = false
)
