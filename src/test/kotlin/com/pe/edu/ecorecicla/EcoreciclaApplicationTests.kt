package com.pe.edu.ecorecicla

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class EcoreciclaApplicationTests(@Autowired private val mockMvc: MockMvc) {
    @Test
    fun everyEndpointExceptLoginRequiresAToken() {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/puntos"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun malformedTokenReturns401() {
        mockMvc.perform(get("/api/v1/puntos").header("Authorization", "Bearer no.es.un.jwt.valido"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
    }

    @Test
    fun invalidCredentialsReturn401() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"usuario":"operario1","contrasena":"incorrecta"}""")
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
    }

    @Test
    fun seededOperatorCanLogInAndReadPoints() {
        val token = login("operario1", "Eco2026*")
        assertTrue(token.count { it == '.' } == 2, "JWT debe tener encabezado, payload y firma")

        mockMvc.perform(get("/api/health").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))

        mockMvc.perform(get("/api/v1/puntos").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.contenido").isArray())
            .andExpect(jsonPath("$.totalElementos").value(12))
            .andExpect(jsonPath("$.pagina").value(0))
    }

    @Test
    fun administratorCannotCreateAWeighing() {
        val token = login("admin", "Eco2026*")
        mockMvc.perform(
            post("/api/v1/puntos/1/pesajes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"material":"Papel","pesoBruto":25.5,"tara":2.5}""")
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
    }

    @Test
    fun backendCalculatesNetWeightInsteadOfAcceptingItFromClient() {
        val token = login("operario1", "Eco2026*")
        mockMvc.perform(
            post("/api/v1/puntos/1/pesajes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"material":"Papel","pesoBruto":25.5,"tara":2.5,"observacion":"Prueba"}""")
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.pesoNeto").value(23.0))
            .andExpect(jsonPath("$.pendiente").value(false))
    }

    @Test
    fun netWeightMustBeGreaterThanZero() {
        val token = login("operario1", "Eco2026*")
        mockMvc.perform(
            post("/api/v1/puntos/1/pesajes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"material":"Papel","pesoBruto":2.5,"tara":2.5}""")
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors.pesoBruto").exists())
    }

    private fun login(usuario: String, contrasena: String): String {
        val body = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"usuario":"$usuario","contrasena":"$contrasena"}""")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.usuario").value(usuario))
            .andReturn().response.contentAsString

        return Regex("\"token\":\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            ?: error("El inicio de sesión debe devolver un JWT")
    }
}
