package com.example.websockets.controllers

import com.example.websockets.dto.*
import com.example.websockets.entities.ChatGroupRepository
import com.example.websockets.entities.ChatUserRepository
import com.example.websockets.services.CustomMinioService
import com.example.websockets.services.RabbitMqService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import java.lang.NullPointerException
import java.security.Principal
import java.time.LocalDateTime

@Controller
class ChatController (
    val messageSender: SimpMessageSendingOperations,
    val customMinioService: CustomMinioService,
    val rabbitService: RabbitMqService,
    val chatGroupRepository: ChatGroupRepository,
    val chatUserRepository: ChatUserRepository,
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

        try {
            // Which exchange is the message going to be published in
            // Defaults to direct msg
            var exchange = "directMsg"

            // Check if receiver exists, avoid dead letters
            // If the receiver is a group set correct exchange
            if (chatMessage.toGroup!!) {
                chatGroupRepository.findByName(chatMessage.receiver)!!
                exchange = "groups/${chatMessage.receiver}"
            }
            else chatUserRepository.findByUsername(chatMessage.receiver)!!

            // Set sender
            chatMessage.sender = principal.name

            // If it's an attachment type msg do claim-check
            if(chatMessage.type == MessageType.ATTACHMENT) {
                messageSender.convertAndSend(
                    "/topic/system/notifications/${principal.name}",
                    UploadFileNotification(
                        NotificationType.UPLOAD_FILE,
                        customMinioService.createPreSignedUrl(chatMessage.content),
                        chatMessage.content
                    )
                )
            }

            // Publish message
            rabbitService.publish(chatMessage, exchange)
        } catch (e : NullPointerException) {
            messageSender.convertAndSend(
                "/topic/system/notifications/${principal.name}",
                GenericNotification(
                    NotificationType.ERROR,
                    "Error sending message to ${chatMessage.receiver}"
                )
            )
        }

    }
}

