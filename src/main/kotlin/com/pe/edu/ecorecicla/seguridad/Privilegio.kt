package com.pe.edu.ecorecicla.seguridad

import jakarta.persistence.*

@Entity
@Table(name = "privilegios")
data class Privilegio(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val nombre: String
)