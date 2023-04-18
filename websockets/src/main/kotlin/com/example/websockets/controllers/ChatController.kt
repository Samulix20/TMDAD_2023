package com.example.websockets.controllers

import com.example.websockets.dto.ChatMessage
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import java.security.Principal

@Controller
class ChatController (
    val messageSender: SimpMessageSendingOperations
) {
    @MessageMapping("/chat.register")
    fun register(@Payload chatMessage: ChatMessage,
                 headerAccessor: SimpMessageHeaderAccessor) {
        messageSender.convertAndSend(
            "/topic/${chatMessage.group}",
            chatMessage
        )
    }

    @MessageMapping("/chat.send")
    fun chat(@Payload chatMessage: ChatMessage,
             headerAccessor: SimpMessageHeaderAccessor,
             principal: Principal) {
        messageSender.convertAndSend(
            "/topic/${chatMessage.group}",
            chatMessage
        )
    }
}

