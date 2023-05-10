package com.example.websockets.controllers

import com.example.websockets.dto.AdminMessage
import com.example.websockets.dto.ChatMessage
import com.example.websockets.dto.MessageType
import com.example.websockets.services.RabbitMqService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.Principal

@RestController
@RequestMapping("/admin")
class AdminController (
    val rabbitService: RabbitMqService
) {
    @PostMapping("/send")
    fun register(
        @RequestBody message: AdminMessage,
        principal : Principal
    ) {
        try {
            rabbitService.publish(
                ChatMessage(
                    sender = principal.name,
                    receiver = "",
                    content = message.content!!,
                    type = MessageType.CHAT
                ), "adminMsg"
            )
        } catch (e: Exception) {
            throw ResponseStatusException(400, "Admin send error", null)
        }
    }
}