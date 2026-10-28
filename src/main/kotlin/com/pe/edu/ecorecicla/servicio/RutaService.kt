package com.pe.edu.ecorecicla.servicio

import com.pe.edu.ecorecicla.config.UsuarioAutenticado
import com.pe.edu.ecorecicla.dominio.EstadoRuta
import com.pe.edu.ecorecicla.dominio.Ruta
import com.pe.edu.ecorecicla.dominio.RutaPunto
import com.pe.edu.ecorecicla.repositorio.PesajeRepository
import com.pe.edu.ecorecicla.repositorio.PuntoAcopioRepository
import com.pe.edu.ecorecicla.repositorio.RutaPuntoRepository
import com.pe.edu.ecorecicla.repositorio.RutaRepository
import com.pe.edu.ecorecicla.repositorio.UsuarioRepository
import com.pe.edu.ecorecicla.web.dto.CrearRutaRequest
import com.pe.edu.ecorecicla.web.dto.OmitirPuntoRequest
import com.pe.edu.ecorecicla.web.dto.RutaDto
import com.pe.edu.ecorecicla.web.dto.RutaPuntoDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RutaService(
    private val rutaRepository: RutaRepository,
    private val rutaPuntoRepository: RutaPuntoRepository,
    private val puntoRepository: PuntoAcopioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val pesajeRepository: PesajeRepository
) {
    @Transactional(readOnly = true)
    fun rutaDelDia(fecha: LocalDate, actor: UsuarioAutenticado): RutaDto {
        var route = rutaRepository.findByFechaYUsuario(fecha, actor.usuario)
        if (route == null && actor.puede("RUTA_CERRAR")) {
            route = rutaRepository.findFirstByFechaAndEstadoOrderByIdAsc(fecha, EstadoRuta.ABIERTA)
                ?: rutaRepository.findAll().firstOrNull { it.fecha == fecha }
        }
        return (route ?: throw ApiException(HttpStatus.NOT_FOUND, "No hay una ruta asignada para $fecha")).toDto()
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long, actor: UsuarioAutenticado): RutaDto {
        val route = obtenerRuta(id)
        if (!route.operario.usuario.equals(actor.usuario, ignoreCase = true) &&
            !actor.puede("RUTA_CERRAR") && !actor.puede("PUNTO_GESTIONAR")) {
            throw ApiException(HttpStatus.FORBIDDEN, "No puedes consultar una ruta asignada a otro usuario")
        }
        return route.toDto()
    }

    @Transactional
    fun crear(request: CrearRutaRequest): RutaDto {
        if (request.puntosIds.distinct().size != request.puntosIds.size) {
            throw ApiException(HttpStatus.BAD_REQUEST, "La ruta no puede repetir un punto", mapOf("puntosIds" to "Elimina identificadores duplicados"))
        }
        val owner = usuarioRepository.findByUsuarioIgnoreCase(request.operarioUsuario.trim())
            ?: throw ApiException(HttpStatus.NOT_FOUND, "No existe el usuario operario ${request.operarioUsuario}")
        if (owner.rol.privilegios.none { it.nombre == "PESAJE_CREAR" }) {
            throw ApiException(HttpStatus.BAD_REQUEST, "El usuario asignado no tiene privilegio para registrar pesajes", mapOf("operarioUsuario" to "Asigna la ruta a un usuario habilitado para registrar pesajes"))
        }
        if (rutaRepository.findByFechaYUsuario(request.fecha, owner.usuario) != null) {
            throw ApiException(HttpStatus.CONFLICT, "Ese usuario ya tiene una ruta para ${request.fecha}")
        }
        val foundPoints = puntoRepository.findAllById(request.puntosIds).associateBy { it.id }
        val missing = request.puntosIds.filterNot { foundPoints.containsKey(it) }
        if (missing.isNotEmpty()) throw ApiException(HttpStatus.NOT_FOUND, "No existen estos puntos: ${missing.joinToString()}")

        val route = rutaRepository.save(Ruta(fecha = request.fecha, estado = EstadoRuta.ABIERTA, operario = owner))
        val links = request.puntosIds.mapIndexed { index, pointId ->
            RutaPunto(ruta = route, punto = requireNotNull(foundPoints[pointId]), secuencia = index + 1)
        }
        rutaPuntoRepository.saveAll(links)
        return obtenerRuta(requireNotNull(route.id)).toDto()
    }

    @Transactional
    fun omitirPunto(rutaId: Long, puntoId: Long, request: OmitirPuntoRequest): RutaDto {
        val route = obtenerRuta(rutaId)
        if (route.estado == EstadoRuta.CERRADA) throw ApiException(HttpStatus.CONFLICT, "No se puede modificar una ruta cerrada")
        val routePoint = rutaPuntoRepository.findByRutaIdAndPuntoId(rutaId, puntoId)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "El punto $puntoId no está planificado en la ruta $rutaId")
        routePoint.motivoOmision = request.motivo.trim()
        rutaPuntoRepository.save(routePoint)
        return route.toDto()
    }

    @Transactional
    fun cerrar(rutaId: Long): RutaDto {
        val route = obtenerRuta(rutaId)
        if (route.estado == EstadoRuta.CERRADA) throw ApiException(HttpStatus.CONFLICT, "La ruta ya está cerrada")
        val links = rutaPuntoRepository.findByRutaOrdenado(rutaId)
        val missing = links.filter { link ->
            pesajeRepository.countNoAnuladosPorRutaPunto(requireNotNull(link.id)) == 0L && link.motivoOmision.isNullOrBlank()
        }
        if (missing.isNotEmpty()) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "No se puede cerrar la ruta: faltan ${missing.size} punto(s) con pesaje o motivo de omisión",
                mapOf("puntosPendientes" to missing.joinToString(", ") { it.punto.razonSocial })
            )
        }
        route.estado = EstadoRuta.CERRADA
        rutaRepository.save(route)
        return route.toDto()
    }

    private fun obtenerRuta(id: Long): Ruta = rutaRepository.findById(id)
        .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "No existe la ruta $id") }

    private fun Ruta.toDto(): RutaDto {
        val routeId = requireNotNull(id)
        val points = rutaPuntoRepository.findByRutaOrdenado(routeId).map { routePoint ->
            val activeWeighings = pesajeRepository.countNoAnuladosPorRutaPunto(requireNotNull(routePoint.id))
            RutaPuntoDto(
                id = requireNotNull(routePoint.punto.id),
                razonSocial = routePoint.punto.razonSocial,
                direccion = routePoint.punto.direccion,
                zona = routePoint.punto.zona,
                materialHabitual = routePoint.punto.materialHabitual,
                secuencia = routePoint.secuencia,
                atendido = activeWeighings > 0 || !routePoint.motivoOmision.isNullOrBlank(),
                motivoOmision = routePoint.motivoOmision
            )
        }
        return RutaDto(
            id = routeId,
            fecha = fecha.toString(),
            estado = estado.name.lowercase(),
            operario = operario.usuario,
            puntos = points
        )
    }
}
