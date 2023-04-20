package com.example.websockets.controllers

import com.example.websockets.dto.AdminMessage
import com.example.websockets.dto.ChatMessage
import com.example.websockets.dto.MessageType
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.Principal

@RestController
@RequestMapping("/admin")
class AdminController (
    val messageSender: SimpMessageSendingOperations
) {
    @PostMapping("/send")
    fun register(
        @RequestBody message: AdminMessage,
        principal : Principal
    ) {
        try {
            messageSender.convertAndSend(
                "/topic/system/admin",
                ChatMessage(
                    sender = principal.name,
                    receiver = "",
                    content = message.content!!,
                    type = MessageType.CHAT
                )
            )
        } catch (e: Exception) {
            throw ResponseStatusException(400, "Admin send error", null)
        }
    }
}