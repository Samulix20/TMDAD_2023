package com.example.websockets.controllers

import com.example.websockets.dto.*
import com.example.websockets.entities.*
import com.example.websockets.services.CustomMinioService
import com.example.websockets.services.RabbitMqService
import com.example.websockets.services.TrendService
import com.nimbusds.jose.shaded.gson.Gson
import org.springframework.data.repository.findByIdOrNull
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.security.core.context.SecurityContextHolder
import java.lang.NullPointerException
import java.security.Principal
import java.time.LocalDateTime

@Controller
class ChatController (
    val messageSender: SimpMessageSendingOperations,
    val customMinioService: CustomMinioService,
    val trendService: TrendService,
    val rabbitService: RabbitMqService,
    val chatGroupRepository: ChatGroupRepository,
    val chatUserRepository: ChatUserRepository,
    val groupMessageRepository: GroupMessageRepository,
) {

    val gson = Gson()

    fun getRequestUser(principal: Principal) : ChatUser {
        return chatUserRepository.findByIdOrNull(SecurityContextHolder.getContext().authentication.details as Long)!!
    }

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
            if(chatMessage.content.length > 500) throw Exception("Message too long")

            // Which exchange is the message going to be published in
            // Defaults to direct msg
            var exchange = "directMsg"
            val group: ChatGroup
            val user = getRequestUser(principal)

            chatMessage.sender = user.username

            // Check if receiver exists, avoid dead letters
            // If the receiver is a group set correct exchange
            if (chatMessage.toGroup!!) {
                group = chatGroupRepository.findByName(chatMessage.receiver)!!
                exchange = "groups.${chatMessage.receiver}"

                if(!rabbitService.getQueueBindings(user.username).contains(group.name)) {
                    throw Exception("User not member of group ${group.name}")
                }
                // Send msg to database
                groupMessageRepository.save(
                    GroupMessage(
                        group = group,
                        content = gson.toJson(chatMessage)
                    )
                )
            }
            else chatUserRepository.findByUsername(chatMessage.receiver)!!

            // If it's an attachment type msg do claim-check
            if(chatMessage.type == MessageType.ATTACHMENT) {
                messageSender.convertAndSend(
                    "/topic/system/notifications/${user.username}",
                    UploadFileNotification(
                        NotificationType.UPLOAD_FILE,
                        customMinioService.createPreSignedUrl(chatMessage.content),
                        chatMessage.content
                    )
                )
            }
            // Store words
            trendService.addMessage(chatMessage)
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

