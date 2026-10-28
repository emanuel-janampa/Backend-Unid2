package com.pe.edu.ecorecicla.dominio

import com.pe.edu.ecorecicla.seguridad.Usuario
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate


enum class EstadoRuta { ABIERTA, CERRADA }

@Entity
@Table(name = "rutas")
class Ruta(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var fecha: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    var estado: EstadoRuta = EstadoRuta.ABIERTA,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "operario_id", nullable = false)
    var operario: Usuario = Usuario()
)
