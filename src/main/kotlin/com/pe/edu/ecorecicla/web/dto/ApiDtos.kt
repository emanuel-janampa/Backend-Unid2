package com.pe.edu.ecorecicla.web.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

// Authentication
 data class LoginRequest(
    @field:NotBlank(message = "El usuario es obligatorio")
    val usuario: String,
    @field:NotBlank(message = "La contraseña es obligatoria")
    val contrasena: String
)

data class LoginResponse(
    val token: String,
    val tipo: String = "Bearer",
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>,
    val expiraEnSegundos: Long = 3600
)

data class UsuarioActualDto(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>
)

// Pagination contract used by every list endpoint.
data class PaginaRespuesta<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)

// Points
 data class PuntoDto(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String
)

data class CrearPuntoRequest(
    @field:NotBlank(message = "La razón social es obligatoria")
    @field:Size(max = 150)
    val razonSocial: String,
    @field:NotBlank(message = "La dirección es obligatoria")
    @field:Size(max = 250)
    val direccion: String,
    @field:NotBlank(message = "La zona es obligatoria")
    @field:Size(max = 80)
    val zona: String,
    @field:NotBlank(message = "El material habitual es obligatorio")
    @field:Size(max = 80)
    val materialHabitual: String
)

// Weighings: net weight is intentionally not accepted from the client. The API calculates it.
data class RegistrarPesajeRequest(
    @field:NotBlank(message = "El material es obligatorio")
    @field:Size(max = 80)
    val material: String,
    @field:NotNull(message = "El peso bruto es obligatorio")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "El peso bruto debe ser mayor que cero")
    val pesoBruto: BigDecimal,
    @field:NotNull(message = "La tara es obligatoria")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "La tara no puede ser negativa")
    val tara: BigDecimal,
    @field:Size(max = 500)
    val observacion: String? = null
)

data class AnularPesajeRequest(
    @field:NotBlank(message = "Debes indicar el motivo de anulación")
    @field:Size(max = 500)
    val motivo: String
)

data class PesajeDto(
    val id: Long,
    val puntoId: Long,
    val rutaId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: BigDecimal,
    val tara: BigDecimal,
    val pesoNeto: BigDecimal,
    val observacion: String?,
    val operario: String,
    val anulado: Boolean,
    val motivoAnulacion: String?,
    val pendiente: Boolean = false
)

// Routes
 data class RutaPuntoDto(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int,
    val atendido: Boolean,
    val motivoOmision: String?
)

data class RutaDto(
    val id: Long,
    val fecha: String,
    val estado: String,
    val operario: String,
    val puntos: List<RutaPuntoDto>
)

data class CrearRutaRequest(
    @field:NotNull(message = "La fecha es obligatoria")
    val fecha: java.time.LocalDate,
    @field:NotBlank(message = "El usuario operario es obligatorio")
    val operarioUsuario: String,
    @field:NotEmpty(message = "La ruta debe incluir al menos un punto")
    @field:Size(max = 120, message = "Una ruta no puede incluir más de 120 puntos")
    val puntosIds: List<Long>
)

data class OmitirPuntoRequest(
    @field:NotBlank(message = "El motivo de omisión es obligatorio")
    @field:Size(max = 500)
    val motivo: String
)

// Standardized error contract required by the exam.
data class ApiErrorDto(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val validationErrors: Map<String, String> = emptyMap()
)
