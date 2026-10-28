package com.pe.edu.ecorecicla.web

import com.pe.edu.ecorecicla.servicio.ApiException
import com.pe.edu.ecorecicla.web.dto.ApiErrorDto
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class ManejadorDeErrores {
    @ExceptionHandler(ApiException::class)
    fun apiException(ex: ApiException, request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(ex.status, ex.message, request, ex.validationErrors)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiErrorDto> {
        val errors = ex.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "Valor inválido") }
        return responder(HttpStatus.BAD_REQUEST, "Datos inválidos", request, errors)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun constraint(ex: ConstraintViolationException, request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(
            HttpStatus.BAD_REQUEST,
            "Parámetros inválidos",
            request,
            ex.constraintViolations.associate { it.propertyPath.toString() to it.message }
        )


    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun tipoParametro(ex: MethodArgumentTypeMismatchException, request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(
            HttpStatus.BAD_REQUEST,
            "El parámetro '${ex.name}' tiene un formato inválido",
            request,
            mapOf(ex.name to "Revisa el tipo de dato enviado")
        )

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun parametroAusente(ex: MissingServletRequestParameterException, request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(
            HttpStatus.BAD_REQUEST,
            "Falta el parámetro obligatorio '${ex.parameterName}'",
            request,
            mapOf(ex.parameterName to "El parámetro es obligatorio")
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun bodyInvalido(request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(HttpStatus.BAD_REQUEST, "El cuerpo JSON está ausente o tiene un formato inválido", request)

    @ExceptionHandler(NoSuchElementException::class)
    fun noEncontrado(ex: NoSuchElementException, request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(HttpStatus.NOT_FOUND, ex.message ?: "Recurso no encontrado", request)

    @ExceptionHandler(AccessDeniedException::class)
    fun sinPrivilegio(request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(HttpStatus.FORBIDDEN, "No tienes el privilegio necesario para esta operación", request)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun conflicto(request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(HttpStatus.CONFLICT, "La operación entra en conflicto con los datos existentes", request)

    @ExceptionHandler(IllegalArgumentException::class)
    fun argumento(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(HttpStatus.BAD_REQUEST, ex.message ?: "Parámetro inválido", request)

    @ExceptionHandler(Exception::class)
    fun errorInesperado(request: HttpServletRequest): ResponseEntity<ApiErrorDto> =
        responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request)

    private fun responder(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
        validationErrors: Map<String, String> = emptyMap()
    ): ResponseEntity<ApiErrorDto> {
        val body = ApiErrorDto(
            timestamp = Instant.now().toString(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI,
            validationErrors = validationErrors
        )
        return ResponseEntity.status(status).body(body)
    }
}
