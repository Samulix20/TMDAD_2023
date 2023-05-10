package com.example.websockets.controllers

import com.example.websockets.dto.*
import com.example.websockets.services.CustomMinioService
import com.example.websockets.services.RabbitMqService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import java.security.Principal
import java.time.LocalDateTime

@Controller
class ChatController (
    val messageSender: SimpMessageSendingOperations,
    val customMinioService: CustomMinioService,
    val rabbitService: RabbitMqService,
) {

    // Start user rabbit session
    @MessageMapping("/chat.start")
    fun start(principal: Principal,
              headerAccessor: SimpMessageHeaderAccessor){

        rabbitService.startUserSession(principal.name, headerAccessor.sessionId!!)
    }

    @MessageMapping("/chat.send")
    fun chat(@Payload chatMessage: ChatMessage,
             principal: Principal) {

        // Which exchange is the message going to be published in
        // Defaults to direct msg
        var exchange = "directMsg"

        // If the receiver is a group set correct exchange
        if (chatMessage.toGroup!!) {
            exchange = "groups/${chatMessage.receiver}"
        }

        // Set sender
        chatMessage.sender = principal.name

        // If it's an attachment type msg do claim-check
        if(chatMessage.type == MessageType.ATTACHMENT) {
            val notification = UploadFileNotification(
                NotificationType.UPLOAD_FILE,
                customMinioService.createPreSignedUrl(chatMessage.content),
                chatMessage.content
            )

            messageSender.convertAndSend(
                "/topic/system/notifications/${principal.name}",
                notification
            )
        }

        // Publish message
        rabbitService.publish(chatMessage, exchange)
    }
}

