package com.pe.edu.ecorecicla.web

import com.pe.edu.ecorecicla.web.dto.ApiErrorDto
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Component
class ApiErrorWriter(private val objectMapper: ObjectMapper) {
    fun write(
        response: HttpServletResponse,
        status: Int,
        error: String,
        message: String,
        path: String,
        validationErrors: Map<String, String> = emptyMap()
    ) {
        if (response.isCommitted) return
        response.status = status
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(
            response.outputStream,
            ApiErrorDto(Instant.now().toString(), status, error, message, path, validationErrors)
        )
    }
}
