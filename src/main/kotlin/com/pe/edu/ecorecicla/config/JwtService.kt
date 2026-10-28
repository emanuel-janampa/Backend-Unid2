package com.pe.edu.ecorecicla.config

import com.pe.edu.ecorecicla.seguridad.Usuario
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

/** Principal reconstructed from the verified JWT. Authorities are privileges, not role names. */
data class UsuarioAutenticado(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>
) : AuthenticatedPrincipal {
    override fun getName(): String = usuario
}

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.issuer}") private val issuer: String,
    @Value("\${app.jwt.expiration-minutes:60}") private val expirationMinutes: Long
) {
    private val signingKey by lazy {
        require(secret.toByteArray(StandardCharsets.UTF_8).size >= 32) {
            "APP_JWT_SECRET debe tener al menos 32 bytes"
        }
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun expiracionEnSegundos(): Long = expirationMinutes.coerceAtLeast(1) * 60

    fun crearToken(usuario: Usuario): String {
        val now = Instant.now()
        val expires = now.plusSeconds(expiracionEnSegundos())
        val privileges = usuario.rol.privilegios.map { it.nombre }.sorted()
        return Jwts.builder()
            .issuer(issuer)
            .subject(usuario.usuario)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expires))
            .claim("userId", requireNotNull(usuario.id))
            .claim("nombre", usuario.nombreCompleto)
            .claim("rol", usuario.rol.nombre)
            .claim("privilegios", privileges)
            .signWith(signingKey)
            .compact()
    }

    fun validarYLeer(token: String): UsuarioAutenticado {
        val claims = Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .payload

        val id = claims["userId"].toString().toLong()
        val usuario = claims.subject ?: error("El token no contiene usuario")
        val nombre = claims["nombre"]?.toString() ?: usuario
        val rol = claims["rol"]?.toString() ?: ""
        val privilegios = (claims["privilegios"] as? Collection<*>)
            .orEmpty()
            .mapNotNull { it?.toString() }
            .toSet()

        return UsuarioAutenticado(id, usuario, nombre, rol, privilegios)
    }
}
