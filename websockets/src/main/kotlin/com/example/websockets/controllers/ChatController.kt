package com.example.websockets.controllers

import com.example.websockets.CustomMinioService
import com.example.websockets.dto.*
import com.example.websockets.entities.ChatGroup
import com.example.websockets.entities.ChatGroupRepository
import com.example.websockets.entities.ChatUserRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.security.core.context.SecurityContextHolder
import java.security.Principal

@Controller
class ChatController (
    val em: EntityManager,
    val messageSender: SimpMessageSendingOperations,
    val chatGroupRepository: ChatGroupRepository,
    val chatUserRepository: ChatUserRepository,
    val customMinioService: CustomMinioService
) {
    @MessageMapping("/chat.send")
    fun chat(@Payload chatMessage: ChatMessage,
             principal: Principal,
             headerAccessor: SimpMessageHeaderAccessor) {

        chatMessage.sender = principal.name

        if(chatMessage.type == MessageType.ATTACHMENT) {
            val urls = customMinioService.createPreSignedUrls(chatMessage.content)
            val notification = UploadFileNotification(
                NotificationType.UPLOAD_FILE,
                urls.first,
                chatMessage.content
            )
            messageSender.convertAndSend(
                "/topic/system/notifications/${principal.name}",
                notification
            )
            chatMessage.content = urls.second
        }

        // TODO: CHECK IF RECEIVER EXISTS

        messageSender.convertAndSend(
            "/topic/chat/${chatMessage.receiver}",
            chatMessage
        )
    }

    @MessageMapping("/chat.createGroup")
    fun createGroup(@Payload message: GroupOperationMessage,
                    principal: Principal,
                    headerAccessor: SimpMessageHeaderAccessor) {
        val userId = SecurityContextHolder.getContext().authentication.details as Long
        val user = chatUserRepository.findByIdOrNull(userId)!!
        val group = ChatGroup(name = message.target!!, admin = user)
        chatGroupRepository.save(group)
        val notif = GenericNotification(NotificationType.GROUP_CREATED, message.target)
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            notif
        )
    }

    @Transactional
    @MessageMapping("/chat.addToGroup")
    fun addToGroup(@Payload message: GroupOperationMessage,
                   principal: Principal,
                   headerAccessor: SimpMessageHeaderAccessor) {
        val userId = SecurityContextHolder.getContext().authentication.details as Long
        val group = chatGroupRepository.findByName(message.target)!!
        if(group.admin.id != userId) {
            throw Exception("Sender is not group admin")
        }
        val user = chatUserRepository.findByUsername(message.name)!!
        user.groups.add(group)
        em.persist(user)
        val notif = GenericNotification(NotificationType.ADDED_TO_GROUP, group.name)
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            notif
        )
    }

    @MessageMapping("/chat.getGroups")
    fun getGroups(principal: Principal,
                  headerAccessor: SimpMessageHeaderAccessor) {
        val userId = SecurityContextHolder.getContext().authentication.details as Long
        val user = chatUserRepository.findByIdOrNull(userId)!!
        val groupNameList = mutableListOf<String>()
        user.groups.forEach { g ->
            groupNameList.add(g.name)
        }
        chatGroupRepository.findByAdmin(user).forEach { g ->
            groupNameList.add(g.name)
        }
        val notif = GroupListNotification(NotificationType.GROUP_LIST, groupNameList)
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            notif
        )
    }
}

