package com.example.websockets.controllers

import com.example.websockets.dto.*
import com.example.websockets.entities.ChatGroup
import com.example.websockets.entities.ChatGroupRepository
import com.example.websockets.entities.ChatUser
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

@Transactional
@Controller
class GroupController (
    val em: EntityManager,
    val messageSender: SimpMessageSendingOperations,
    val chatGroupRepository: ChatGroupRepository,
    val chatUserRepository: ChatUserRepository,
    val rabbitService: RabbitMqService,
) {
    fun getRequestUser(principal: Principal) : ChatUser {
        return chatUserRepository.findByIdOrNull(SecurityContextHolder.getContext().authentication.details as Long)!!
    }

    fun adminCheck(user: ChatUser, group: ChatGroup) {
        if(user.id != group.admin.id) throw Exception("Sender is not group admin")
    }

    @MessageMapping("/chat.createGroup")
    fun createGroup(@Payload message: GroupOperationMessage,
                    principal: Principal) {

        val user = getRequestUser(principal)
        val group = ChatGroup(name = message.target!!, admin = user)
        chatGroupRepository.save(group)
        // Create group in rabbit
        rabbitService.createGroupExchange(group.name, user.username)
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GenericNotification(NotificationType.GENERIC,
                "${message.target} created")
        )
    }

    @MessageMapping("/chat.deleteGroup")
    fun deleteGroup(@Payload message: GroupOperationMessage,
                    principal: Principal) {

        val user = getRequestUser(principal)
        val group = chatGroupRepository.findByName(message.target!!)!!
        adminCheck(user, group)
        chatGroupRepository.delete(group)
        rabbitService.deleteGroupExchange(group.name)
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GenericNotification(NotificationType.GENERIC,
                "${message.target} deleted")
        )
    }

    @MessageMapping("/chat.addToGroup")
    fun addToGroup(@Payload message: GroupOperationMessage,
                   principal: Principal) {

        val admin = getRequestUser(principal)
        val group = chatGroupRepository.findByName(message.target)!!
        adminCheck(admin, group)
        val user = chatUserRepository.findByUsername(message.name)!!
        user.groups.add(group)
        em.persist(user)
        // Bind user to group in rabbit
        rabbitService.bindUserToGroup(group.name, user.username)

        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GenericNotification(
                NotificationType.GENERIC,
                "Added to group ${group.name}"
            )
        )
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GenericNotification(
                NotificationType.GENERIC,
                "${user.username} added to group ${group.name}"
            )
        )
    }

    @MessageMapping("/chat.removeFromGroup")
    fun removeFromGroup(@Payload message: GroupOperationMessage,
                        principal: Principal) {
        val admin = getRequestUser(principal)
        val group = chatGroupRepository.findByName(message.target)!!
        adminCheck(admin, group)

        val removed = group.users.removeIf{ it.username == message.name!! }
        if(removed) {
            // Save change in database
            em.persist(group)
            // Remove binding from rabbit
            rabbitService.unbindUserFromGroup(group.name, message.name!!)
        }

        messageSender.convertAndSend(
            "/topic/system/notifications/${admin.username}",
            GenericNotification(
                NotificationType.GENERIC,
                "$removed"
            )
        )
    }

    @MessageMapping("/chat.getGroups")
    fun getGroups(principal: Principal) {

        val user = getRequestUser(principal)
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

