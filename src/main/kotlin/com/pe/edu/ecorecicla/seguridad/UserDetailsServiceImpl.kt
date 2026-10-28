package com.pe.edu.ecorecicla.seguridad

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import pe.edu.ecorecicla.repositorio.UsuarioRepository

@Service
class UserDetailsServiceImpl(
    private val usuarioRepository: UsuarioRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("Usuario no encontrado: $username") }

        // Mapear rol y privilegios como autoridades de Spring Security
        val authorities = mutableListOf<SimpleGrantedAuthority>()
        authorities.add(SimpleGrantedAuthority("ROLE_${usuario.rol.nombre}"))
        usuario.rol.privilegios.forEach { privilegio ->
            authorities.add(SimpleGrantedAuthority(privilegio.nombre))
        }

        return User(
            usuario.username,
            usuario.passwordHash,
            authorities
        )
    }
}