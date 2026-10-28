package com.pe.edu.ecorecicla.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.edu.ecorecicla.dominio.PuntoAcopio

@Repository
interface PuntoAcopioRepository : JpaRepository<PuntoAcopio, Long> {
}