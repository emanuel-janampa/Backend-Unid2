package com.pe.edu.ecorecicla.dominio

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "puntos_acopio")
class PuntoAcopio(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "razon_social", nullable = false, length = 150)
    var razonSocial: String = "",

    @Column(nullable = false, length = 250)
    var direccion: String = "",

    @Column(nullable = false, length = 80)
    var zona: String = "",

    @Column(name = "material_habitual", nullable = false, length = 80)
    var materialHabitual: String = ""
)
