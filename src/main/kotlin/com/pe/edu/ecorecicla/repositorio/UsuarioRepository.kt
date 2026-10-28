package com.pe.edu.ecorecicla.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.edu.ecorecicla.seguridad.Usuario
import java.util.Optional

@Repository
interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByUsername(username: String): Optional<Usuario>
}