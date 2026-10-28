package com.pe.edu.ecorecicla.seguridad

import jakarta.persistence.*

@Entity
@Table(name = "roles")
data class Rol(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val nombre: String,
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "rol_privilegios",
        joinColumns = [JoinColumn(name = "rol_id")],
        inverseJoinColumns = [JoinColumn(name = "privilegio_id")]
    )
    val privilegios: Set<Privilegio> = HashSet()
)