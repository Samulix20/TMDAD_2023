package com.example.websockets.security

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class WebsocketsAuthorizationInterceptor (
    val tokenService : TokenService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val headerAccessor = MessageHeaderAccessor.getAccessor(message) as StompHeaderAccessor

        if(StompCommand.CONNECT == headerAccessor.command) {
            // Throws exception if invalid token
            val jwt = tokenService.jwt(headerAccessor.getNativeHeader("token")!![0])
            headerAccessor.user = UsernamePasswordAuthenticationToken(
                jwt.subject,
                "",
                listOf(SimpleGrantedAuthority("USER"))
            )
        }

        return message
    }
}