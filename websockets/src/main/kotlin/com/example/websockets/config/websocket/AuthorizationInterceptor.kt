package com.example.websockets.config.websocket

import com.example.websockets.services.TokenService
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

@Component
class AuthorizationInterceptor (
    val tokenService : TokenService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val headerAccessor = MessageHeaderAccessor.getAccessor(message) as StompHeaderAccessor

        if(StompCommand.CONNECT == headerAccessor.command) {
            // Throws exception if invalid token
            try {
                val jwt = tokenService.jwt(headerAccessor.getNativeHeader("token")!![0])
                headerAccessor.user = UsernamePasswordAuthenticationToken(
                    jwt.subject,
                    "",
                    listOf(SimpleGrantedAuthority("USER"))
                )
            } catch (e : JwtException) {
                throw Exception("Websocket authorization error")
            }
        }
        return message
    }
}