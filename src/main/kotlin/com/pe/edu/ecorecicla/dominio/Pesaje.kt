package com.pe.edu.ecorecicla.dominio

import jakarta.persistence.*

@Entity
@Table(name = "pesajes")
data class Pesaje(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val puntoId: Long,

    @Column(nullable = false)
    val fechaHora: String,

    @Column(nullable = false)
    val material: String,

    @Column(nullable = false)
    val pesoBruto: Double,

    @Column(nullable = false)
    val tara: Double,

    @Column(nullable = false)
    val pesoNeto: Double,

    @Column(nullable = false)
    val operarioUsername: String,

    @Column(nullable = false)
    val estado: String,

    @Column(nullable = true)
    val motivoAnulacion: String? = null,

    @Column(nullable = false)
    val pendiente: Boolean = false
)