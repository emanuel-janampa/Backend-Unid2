package com.pe.edu.ecorecicla.dominio

import com.pe.edu.ecorecicla.seguridad.Usuario
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "pesajes")
class Pesaje(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_punto_id", nullable = false)
    var rutaPunto: RutaPunto = RutaPunto(),

    @Column(name = "fecha_hora", nullable = false)
    var fechaHora: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, length = 80)
    var material: String = "",

    @Column(name = "peso_bruto", nullable = false, precision = 12, scale = 3)
    var pesoBruto: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 12, scale = 3)
    var tara: BigDecimal = BigDecimal.ZERO,

    @Column(name = "peso_neto", nullable = false, precision = 12, scale = 3)
    var pesoNeto: BigDecimal = BigDecimal.ZERO,

    @Column(length = 500)
    var observacion: String? = null,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    var registradoPor: Usuario = Usuario(),

    @Column(nullable = false)
    var anulado: Boolean = false,

    @Column(name = "motivo_anulacion", length = 500)
    var motivoAnulacion: String? = null
)
