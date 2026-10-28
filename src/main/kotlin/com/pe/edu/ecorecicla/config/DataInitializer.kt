package com.pe.edu.ecorecicla.config

import com.pe.edu.ecorecicla.dominio.EstadoRuta
import com.pe.edu.ecorecicla.dominio.Pesaje
import com.pe.edu.ecorecicla.dominio.PuntoAcopio
import com.pe.edu.ecorecicla.dominio.Ruta
import com.pe.edu.ecorecicla.dominio.RutaPunto
import com.pe.edu.ecorecicla.repositorio.PesajeRepository
import com.pe.edu.ecorecicla.repositorio.PrivilegioRepository
import com.pe.edu.ecorecicla.repositorio.PuntoAcopioRepository
import com.pe.edu.ecorecicla.repositorio.RolRepository
import com.pe.edu.ecorecicla.repositorio.RutaPuntoRepository
import com.pe.edu.ecorecicla.repositorio.RutaRepository
import com.pe.edu.ecorecicla.repositorio.UsuarioRepository
import com.pe.edu.ecorecicla.seguridad.Privilegio
import com.pe.edu.ecorecicla.seguridad.Rol
import com.pe.edu.ecorecicla.seguridad.Usuario
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Loads the exact demo users and enough sample records to exercise all business rules. */
@Component
class DataInitializer(
    private val privilegeRepository: PrivilegioRepository,
    private val roleRepository: RolRepository,
    private val userRepository: UsuarioRepository,
    private val pointRepository: PuntoAcopioRepository,
    private val routeRepository: RutaRepository,
    private val routePointRepository: RutaPuntoRepository,
    private val weighRepository: PesajeRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.seed.enabled:true}") private val seedEnabled: Boolean
) : CommandLineRunner {
    @Transactional
    override fun run(vararg args: String) {
        if (!seedEnabled || userRepository.count() > 0L) return

        val privilegeNames = listOf(
            "PUNTO_LEER", "PESAJE_LEER", "PESAJE_CREAR", "PESAJE_EDITAR",
            "PESAJE_ANULAR", "RUTA_CERRAR", "PUNTO_GESTIONAR"
        )
        val privileges = privilegeNames.associateWith { name ->
            privilegeRepository.findByNombre(name) ?: privilegeRepository.save(Privilegio(nombre = name))
        }

        fun newRole(name: String, names: List<String>) = roleRepository.save(
            Rol(nombre = name, privilegios = names.map { requireNotNull(privileges[it]) }.toMutableSet())
        )

        val operarioRole = newRole(
            "OPERARIO",
            listOf("PUNTO_LEER", "PESAJE_LEER", "PESAJE_CREAR", "PESAJE_EDITAR")
        )
        val supervisorRole = newRole(
            "SUPERVISOR",
            listOf("PUNTO_LEER", "PESAJE_LEER", "PESAJE_CREAR", "PESAJE_EDITAR", "PESAJE_ANULAR", "RUTA_CERRAR")
        )
        val adminRole = newRole("ADMINISTRADOR", listOf("PUNTO_LEER", "PESAJE_LEER", "PUNTO_GESTIONAR"))

        val operario = userRepository.save(
            Usuario(usuario = "operario1", nombreCompleto = "Carlos Operario", passwordHash = requireNotNull(passwordEncoder.encode("Eco2026*")), rol = operarioRole)
        )
        val supervisor = userRepository.save(
            Usuario(usuario = "supervisor1", nombreCompleto = "María Supervisora", passwordHash = requireNotNull(passwordEncoder.encode("Eco2026*")), rol = supervisorRole)
        )
        userRepository.save(
            Usuario(usuario = "admin", nombreCompleto = "Administrador EcoRecicla", passwordHash = requireNotNull(passwordEncoder.encode("Eco2026*")), rol = adminRole)
        )

        val definitions = listOf(
            Triple("Comercial Andina S.A.C.", "Av. Los Álamos 120", "Norte") to "Papel",
            Triple("Mercados Unidos S.A.", "Jr. Las Flores 245", "Norte") to "Cartón",
            Triple("Textiles del Pacífico", "Calle Río Verde 880", "Norte") to "Plástico",
            Triple("Distribuidora Sol", "Av. Industrial 510", "Norte") to "Papel",
            Triple("Restaurantes Buen Sabor", "Calle Central 64", "Centro") to "Vidrio",
            Triple("Oficinas Horizonte", "Av. República 1001", "Centro") to "Papel",
            Triple("Clínica San Gabriel", "Jr. Los Cedros 455", "Centro") to "Cartón",
            Triple("Hotel Mirador", "Av. Los Próceres 78", "Centro") to "Plástico",
            Triple("Almacenes del Sur", "Carretera Sur km 4", "Sur") to "Cartón",
            Triple("Fábrica Nueva Vida", "Av. Progreso 160", "Sur") to "Plástico",
            Triple("Colegio Los Pinos", "Calle Escolar 20", "Sur") to "Papel",
            Triple("Centro Empresarial del Valle", "Av. El Valle 321", "Sur") to "Vidrio"
        )
        val points = pointRepository.saveAll(definitions.map { (companyAndAddressAndZone, material) ->
            PuntoAcopio(
                razonSocial = companyAndAddressAndZone.first,
                direccion = companyAndAddressAndZone.second,
                zona = companyAndAddressAndZone.third,
                materialHabitual = material
            )
        })

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val todayRoute = routeRepository.save(Ruta(fecha = today, estado = EstadoRuta.ABIERTA, operario = operario))
        val yesterdayRoute = routeRepository.save(Ruta(fecha = yesterday, estado = EstadoRuta.CERRADA, operario = operario))
        val todayLinks = routePointRepository.saveAll(points.take(6).mapIndexed { index, point ->
            RutaPunto(ruta = todayRoute, punto = point, secuencia = index + 1)
        })
        val yesterdayLinks = routePointRepository.saveAll(points.drop(6).take(6).mapIndexed { index, point ->
            RutaPunto(ruta = yesterdayRoute, punto = point, secuencia = index + 1)
        })

        // Six old points all have at least one live weigh-in, so the closed route has a complete history.
        yesterdayLinks.forEachIndexed { index, link ->
            crearPesaje(link, yesterday, index, operario, anulado = false)
        }
        // Five additional historical weighings bring the total to 11; one is annulled.
        // A live weighing below is created by the supervisor to exercise RN-02 on today's route.
        yesterdayLinks.take(5).forEachIndexed { index, link ->
            crearPesaje(
                link = link,
                fecha = yesterday,
                secuencia = index + 10,
                autor = operario,
                anulado = index == 0,
                motivoAnulacion = if (index == 0) "Registro duplicado de demostración" else null
            )
        }
        // The open route has four weighed points and two remaining, so RN-04 can be demonstrated.
        todayLinks.take(4).forEachIndexed { index, link ->
            // One live weigh-in belongs to supervisor1 so operario1 can demonstrate RN-02 (403 for another user's row).
            crearPesaje(link, today, index + 20, if (index == 2) supervisor else operario, anulado = false)
        }
    }

    private fun crearPesaje(
        link: RutaPunto,
        fecha: LocalDate,
        secuencia: Int,
        autor: Usuario,
        anulado: Boolean = false,
        motivoAnulacion: String? = null
    ) {
        val gross = BigDecimal("${18 + (secuencia % 7)}.500")
        val tare = BigDecimal("2.500")
        weighRepository.save(
            Pesaje(
                rutaPunto = link,
                fechaHora = LocalDateTime.of(fecha, LocalTime.of(8, 0)).plusMinutes(secuencia.toLong() * 7L),
                material = link.punto.materialHabitual,
                pesoBruto = gross,
                tara = tare,
                pesoNeto = gross.subtract(tare),
                observacion = if (anulado) "Registro de ejemplo para anulación" else null,
                registradoPor = autor,
                anulado = anulado,
                motivoAnulacion = motivoAnulacion
            )
        )
    }
}
