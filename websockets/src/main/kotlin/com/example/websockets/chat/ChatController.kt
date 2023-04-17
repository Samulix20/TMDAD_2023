package com.example.websockets.chat

import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.HttpClientErrorException.Unauthorized
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.net.PasswordAuthentication
import java.security.Principal

@Controller
class ChatController (
    val messageSender: SimpMessageSendingOperations
) {
    @MessageMapping("/chat.register")
    fun register(@Payload chatMessage: ChatMessage,
                 headerAccessor: SimpMessageHeaderAccessor) {

        println(SecurityContextHolder.getContext().authentication)

        messageSender.convertAndSend(
            "/topic/${chatMessage.group}",
            chatMessage
        )
    }

    @MessageMapping("/chat.send")
    fun chat(@Payload chatMessage: ChatMessage,
             headerAccessor: SimpMessageHeaderAccessor,
             principal: Principal) {

        println(SecurityContextHolder.getContext().authentication)

        messageSender.convertAndSend(
            "/topic/${chatMessage.group}",
            chatMessage
        )
    }
}

