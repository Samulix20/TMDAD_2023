package com.example.websockets.controllers

import com.example.websockets.dto.*
import com.example.websockets.entities.ChatGroup
import com.example.websockets.entities.ChatGroupRepository
import com.example.websockets.entities.ChatUserRepository
import com.example.websockets.services.RabbitMqService
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.security.core.context.SecurityContextHolder
import java.security.Principal

@Controller
class GroupController (
    val em: EntityManager,
    val messageSender: SimpMessageSendingOperations,
    val chatGroupRepository: ChatGroupRepository,
    val chatUserRepository: ChatUserRepository,
    val rabbitService: RabbitMqService,
) {
    @MessageMapping("/chat.createGroup")
    fun createGroup(@Payload message: GroupOperationMessage,
                    principal: Principal) {

        val userId = SecurityContextHolder.getContext().authentication.details as Long
        val user = chatUserRepository.findByIdOrNull(userId)!!
        val group = ChatGroup(name = message.target!!, admin = user)
        chatGroupRepository.save(group)
        // Create group in rabbit
        rabbitService.createGroupExchange(group.name, user.username)
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GenericNotification(NotificationType.GROUP_CREATED, message.target)
        )
    }

    @Transactional
    @MessageMapping("/chat.addToGroup")
    fun addToGroup(@Payload message: GroupOperationMessage,
                   principal: Principal) {

        val userId = SecurityContextHolder.getContext().authentication.details as Long
        val group = chatGroupRepository.findByName(message.target)!!
        if(group.admin.id != userId) {
            throw Exception("Sender is not group admin")
        }
        val user = chatUserRepository.findByUsername(message.name)!!
        user.groups.add(group)
        em.persist(user)
        // Bind user to group in rabbit
        rabbitService.bindUserToGroup(group.name, user.username)
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GenericNotification(NotificationType.ADDED_TO_GROUP, group.name)
        )
    }

    @MessageMapping("/chat.getGroups")
    fun getGroups(principal: Principal) {

        val userId = SecurityContextHolder.getContext().authentication.details as Long
        val user = chatUserRepository.findByIdOrNull(userId)!!
        val groupNameList = mutableListOf<String>()
        user.groups.forEach { g ->
            groupNameList.add(g.name)
        }
        chatGroupRepository.findByAdmin(user).forEach { g ->
            groupNameList.add(g.name)
        }
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GroupListNotification(NotificationType.GROUP_LIST, groupNameList)
        )
    }
}

