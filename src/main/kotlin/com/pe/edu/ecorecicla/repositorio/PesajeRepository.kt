package com.pe.edu.ecorecicla.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.edu.ecorecicla.dominio.Pesaje

@Repository
interface PesajeRepository : JpaRepository<Pesaje, Long> {
}