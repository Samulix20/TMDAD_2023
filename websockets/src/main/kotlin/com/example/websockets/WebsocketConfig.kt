package com.example.websockets

import com.example.websockets.security.TokenService
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

@Component
class WsInterceptor (
    val tokenService : TokenService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val headerAccessor = MessageHeaderAccessor.getAccessor(message) as StompHeaderAccessor

        if(StompCommand.CONNECT == headerAccessor.command) {
            val jwt = tokenService.jwt(headerAccessor.getNativeHeader("token")!![0])
            val authToken = UsernamePasswordAuthenticationToken(jwt.subject, "", listOf(SimpleGrantedAuthority("USER")))
            headerAccessor.user = authToken
        }

        else if(StompCommand.SUBSCRIBE == headerAccessor.command) {
            println(headerAccessor)
        }

        return message
    }
}

class AssignPrincipal : DefaultHandshakeHandler() {

    companion object {
        const val ATTR_PRINCIPAL = "__principal__"
    }

    override fun determineUser(request: ServerHttpRequest,
                               wsHandler: WebSocketHandler,
                               attributes: MutableMap<String?, Any?>): Principal {
        val name =  "undf"
        return Principal { return@Principal name }
    }
}

@Configuration
@EnableWebSocketMessageBroker
class WebsocketConfig (
    val interceptor: WsInterceptor
) : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/websocket").setAllowedOriginPatterns("*").setHandshakeHandler(AssignPrincipal()).withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(interceptor)
    }
}
