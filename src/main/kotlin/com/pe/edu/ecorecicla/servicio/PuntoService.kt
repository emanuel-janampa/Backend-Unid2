package com.pe.edu.ecorecicla.servicio

import com.pe.edu.ecorecicla.dominio.PuntoAcopio
import com.pe.edu.ecorecicla.repositorio.PuntoAcopioRepository
import com.pe.edu.ecorecicla.web.dto.CrearPuntoRequest
import com.pe.edu.ecorecicla.web.dto.PaginaRespuesta
import com.pe.edu.ecorecicla.web.dto.PuntoDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PuntoService(private val puntoRepository: PuntoAcopioRepository) {
    @Transactional(readOnly = true)
    fun listar(page: Int, size: Int, zona: String?): PaginaRespuesta<PuntoDto> {
        validarPaginacion(page, size)
        val pageResult = if (zona.isNullOrBlank()) {
            puntoRepository.findAll(PageRequest.of(page, size))
        } else {
            puntoRepository.findByZonaIgnoreCase(zona.trim(), PageRequest.of(page, size))
        }
        return pageResult.toRespuesta { it.toDto() }
    }

    @Transactional
    fun crear(request: CrearPuntoRequest): PuntoDto {
        val saved = puntoRepository.save(
            PuntoAcopio(
                razonSocial = request.razonSocial.trim(),
                direccion = request.direccion.trim(),
                zona = request.zona.trim(),
                materialHabitual = request.materialHabitual.trim()
            )
        )
        return saved.toDto()
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long): PuntoAcopio = puntoRepository.findById(id)
        .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "No existe el punto de acopio $id") }

    private fun validarPaginacion(page: Int, size: Int) {
        if (page < 0) throw ApiException(HttpStatus.BAD_REQUEST, "El parámetro page no puede ser negativo", mapOf("page" to "Debe ser 0 o mayor"))
        if (size !in 1..100) throw ApiException(HttpStatus.BAD_REQUEST, "El parámetro size debe estar entre 1 y 100", mapOf("size" to "Usa un tamaño de 1 a 100"))
    }
}

fun PuntoAcopio.toDto() = PuntoDto(
    id = requireNotNull(id),
    razonSocial = razonSocial,
    direccion = direccion,
    zona = zona,
    materialHabitual = materialHabitual
)

internal fun <A : Any, B> Page<A>.toRespuesta(convertir: (A) -> B) = PaginaRespuesta(
    contenido = content.map(convertir),
    pagina = number,
    tamanio = size,
    totalElementos = totalElements,
    totalPaginas = totalPages,
    ultima = isLast
)
