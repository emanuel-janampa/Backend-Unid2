package com.pe.edu.ecorecicla.servicio

import org.springframework.http.HttpStatus

class ApiException(
    val status: HttpStatus,
    override val message: String,
    val validationErrors: Map<String, String> = emptyMap()
) : RuntimeException(message)
