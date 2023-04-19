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
    @MessageMapping("/chat.send")
    fun chat(@Payload chatMessage: ChatMessage,
             principal: Principal,
             headerAccessor: SimpMessageHeaderAccessor) {
        chatMessage.sender = principal.name
        messageSender.convertAndSend(
            "/topic/${chatMessage.receiver}",
            chatMessage
        )
    }
}

