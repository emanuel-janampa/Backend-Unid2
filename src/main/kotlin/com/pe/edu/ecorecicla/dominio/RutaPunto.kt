package com.pe.edu.ecorecicla.dominio

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "ruta_puntos",
    uniqueConstraints = [UniqueConstraint(name = "uk_ruta_punto", columnNames = ["ruta_id", "punto_id"])]
)
class RutaPunto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    var ruta: Ruta = Ruta(),

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "punto_id", nullable = false)
    var punto: PuntoAcopio = PuntoAcopio(),

    @Column(nullable = false)
    var secuencia: Int = 1,

    @Column(name = "motivo_omision", length = 500)
    var motivoOmision: String? = null
)
