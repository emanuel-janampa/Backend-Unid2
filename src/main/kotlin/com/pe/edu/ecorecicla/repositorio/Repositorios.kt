package com.pe.edu.ecorecicla.repositorio

import com.pe.edu.ecorecicla.dominio.EstadoRuta
import com.pe.edu.ecorecicla.dominio.Pesaje
import com.pe.edu.ecorecicla.dominio.PuntoAcopio
import com.pe.edu.ecorecicla.dominio.Ruta
import com.pe.edu.ecorecicla.dominio.RutaPunto
import com.pe.edu.ecorecicla.seguridad.Privilegio
import com.pe.edu.ecorecicla.seguridad.Rol
import com.pe.edu.ecorecicla.seguridad.Usuario
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface PrivilegioRepository : JpaRepository<Privilegio, Long> {
    fun findByNombre(nombre: String): Privilegio?
}

interface RolRepository : JpaRepository<Rol, Long> {
    fun findByNombre(nombre: String): Rol?
}

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByUsuarioIgnoreCase(usuario: String): Usuario?
    fun existsByUsuarioIgnoreCase(usuario: String): Boolean
}

interface PuntoAcopioRepository : JpaRepository<PuntoAcopio, Long> {
    fun findByZonaIgnoreCase(zona: String, pageable: Pageable): Page<PuntoAcopio>
}

interface RutaRepository : JpaRepository<Ruta, Long> {
    @Query("select r from Ruta r where r.fecha = :fecha and lower(r.operario.usuario) = lower(:usuario)")
    fun findByFechaYUsuario(@Param("fecha") fecha: LocalDate, @Param("usuario") usuario: String): Ruta?

    fun findFirstByFechaAndEstadoOrderByIdAsc(fecha: LocalDate, estado: EstadoRuta): Ruta?
}

interface RutaPuntoRepository : JpaRepository<RutaPunto, Long> {
    @Query("select rp from RutaPunto rp join fetch rp.ruta where rp.ruta.id = :rutaId order by rp.secuencia asc")
    fun findByRutaOrdenado(@Param("rutaId") rutaId: Long): List<RutaPunto>

    @Query("select rp from RutaPunto rp join fetch rp.ruta where rp.punto.id = :puntoId and rp.ruta.fecha = :fecha and rp.ruta.estado = :estado order by rp.secuencia asc")
    fun findForPointAndDate(
        @Param("puntoId") puntoId: Long,
        @Param("fecha") fecha: LocalDate,
        @Param("estado") estado: EstadoRuta
    ): List<RutaPunto>

    @Query("select rp from RutaPunto rp where rp.ruta.id = :rutaId and rp.punto.id = :puntoId")
    fun findByRutaIdAndPuntoId(@Param("rutaId") rutaId: Long, @Param("puntoId") puntoId: Long): RutaPunto?

}

interface PesajeRepository : JpaRepository<Pesaje, Long> {
    @Query("select p from Pesaje p join p.rutaPunto rp where rp.punto.id = :puntoId order by p.fechaHora desc")
    fun findByPuntoOrdenado(
        @Param("puntoId") puntoId: Long,
        pageable: Pageable
    ): Page<Pesaje>

    @Query("select p from Pesaje p where p.rutaPunto.id = :rutaPuntoId and p.anulado = false")
    fun findNoAnuladosPorRutaPunto(@Param("rutaPuntoId") rutaPuntoId: Long): List<Pesaje>

    @Query("select count(p) from Pesaje p where p.rutaPunto.id = :rutaPuntoId and p.anulado = false")
    fun countNoAnuladosPorRutaPunto(@Param("rutaPuntoId") rutaPuntoId: Long): Long
}
