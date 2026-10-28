package com.pe.edu.ecorecicla.dominio

import jakarta.persistence.*
import com.pe.edu.ecorecicla.seguridad.Usuario

@Entity
@Table(name = "rutas")
data class Ruta(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val fecha: String,

    @Column(nullable = false)
    val cerrada: Boolean = false,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    val operario: Usuario,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinColumn(name = "ruta_id")
    val puntos: List<PuntoAcopio> = ArrayList()
)