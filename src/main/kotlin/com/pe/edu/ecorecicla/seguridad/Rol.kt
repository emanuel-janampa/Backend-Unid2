package com.pe.edu.ecorecicla.seguridad

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "roles")
class Rol(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 40)
    var nombre: String = "",

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "rol_privilegios",
        joinColumns = [JoinColumn(name = "rol_id")],
        inverseJoinColumns = [JoinColumn(name = "privilegio_id")]
    )
    var privilegios: MutableSet<Privilegio> = mutableSetOf()
)
