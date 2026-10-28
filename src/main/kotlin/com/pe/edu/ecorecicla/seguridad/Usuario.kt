package com.pe.edu.ecorecicla.seguridad

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "usuarios")
class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var usuario: String = "",

    @Column(name = "nombre_completo", nullable = false, length = 120)
    var nombreCompleto: String = "",

    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String = "",

    @Column(nullable = false)
    var activo: Boolean = true,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    var rol: Rol = Rol()
)
