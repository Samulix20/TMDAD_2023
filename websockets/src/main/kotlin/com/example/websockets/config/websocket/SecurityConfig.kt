package com.example.websockets.config.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.SimpMessageType
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer

// TODO: FIX CSRF TOKEN AND UPDATE TO LATEST VERSION
@Configuration
class SecurityConfig : AbstractSecurityWebSocketMessageBrokerConfigurer() {
    override fun configureInbound(messages: MessageSecurityMetadataSourceRegistry) {
        messages.simpTypeMatchers(SimpMessageType.CONNECT).permitAll()
            .simpTypeMatchers(SimpMessageType.DISCONNECT).permitAll()
            .anyMessage().authenticated()
    }

    override fun sameOriginDisabled(): Boolean {
        return true
    }
}