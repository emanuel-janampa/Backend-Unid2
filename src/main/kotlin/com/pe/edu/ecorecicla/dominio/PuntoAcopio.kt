package com.pe.edu.ecorecicla.dominio

import jakarta.persistence.*

@Entity
@Table(name = "puntos_acopio")
data class PuntoAcopio(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val razonSocial: String,

    @Column(nullable = false)
    val direccion: String,

    @Column(nullable = false)
    val zona: String,

    @Column(nullable = false)
    val materialHabitual: String,

    @Column(nullable = false)
    val secuencia: Int,

    @Column(nullable = false)
    val atendido: Boolean = false
)