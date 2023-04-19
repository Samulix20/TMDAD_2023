package com.example.websockets.services;

import com.example.websockets.entities.ChatUser
import com.example.websockets.entities.ChatUserRepository
import org.apache.tomcat.util.http.parser.Authorization
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TokenService (
    val jwtDecoder: JwtDecoder,
    val jwtEncoder: JwtEncoder,
    val userService: ChatUserRepository
) {
    fun createToken(user: ChatUser): String {
        val jwsHeader = JwsHeader.with{ "HS256" }.build()
        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(30L, ChronoUnit.DAYS))
            .subject(user.username)
            .claim("userId", user.id)
            .build()
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }

    fun userFromToken(token: String): ChatUser? {
        return try {
            val jwt = jwtDecoder.decode(token)
            val userId = jwt.claims["userId"] as Long
            userService.findByIdOrNull(userId)
        } catch (e: Exception) {
            null
        }
    }

    fun authorizationFromToken(token: String) : UsernamePasswordAuthenticationToken {
        val user = userFromToken(token)!!
        return UsernamePasswordAuthenticationToken(
            user.username, "",
            listOf(SimpleGrantedAuthority("ROLE_"+user.role))
        )
    }
}