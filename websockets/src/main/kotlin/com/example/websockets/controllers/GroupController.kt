package com.example.websockets.controllers

import com.example.websockets.dto.*
import com.example.websockets.entities.*
import com.example.websockets.services.RabbitMqService
import com.nimbusds.jose.shaded.gson.Gson
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
    val messageSender: SimpMessageSendingOperations,
    val chatGroupRepository: ChatGroupRepository,
    val chatUserRepository: ChatUserRepository,
    val groupMessageRepository: GroupMessageRepository,
    val rabbitService: RabbitMqService
) {
    val gson = Gson()

    fun getRequestUser(principal: Principal) : ChatUser {
        return chatUserRepository.findByIdOrNull(SecurityContextHolder.getContext().authentication.details as Long)!!
    }

    fun getRequestGroup(message: GroupOperationMessage) : ChatGroup {
        return chatGroupRepository.findByName(message.target!!)!!
    }

    fun adminCheck(user: ChatUser, group: ChatGroup) {
        if(user.id != group.admin.id) throw Exception("Sender is not group admin")
    }

    @MessageMapping("/group.create")
    fun createGroup(@Payload message: GroupOperationMessage,
                    principal: Principal) {
        val user = getRequestUser(principal)
        val group = ChatGroup(name = message.target!!, admin = user)

        // Save group un DB
        chatGroupRepository.save(group)
        // Create group in rabbit
        rabbitService.createGroupExchange(group.name, user.username)

        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GenericNotification(
                NotificationType.GENERIC,
                "${message.target} created"
            )
        )
    }

    @MessageMapping("/group.delete")
    fun deleteGroup(@Payload message: GroupOperationMessage,
                    principal: Principal) {
        val user = getRequestUser(principal)
        val group = getRequestGroup(message)
        adminCheck(user, group)

        // Remove group in DB
        chatGroupRepository.delete(group)
        // Delete from rabbit
        rabbitService.deleteGroupExchange(group.name)

        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GenericNotification(
                NotificationType.GENERIC,
                "${message.target} deleted"
            )
        )
    }

    @MessageMapping("/group.addUser")
    fun addToGroup(@Payload message: GroupOperationMessage,
                   principal: Principal) {
        val admin = getRequestUser(principal)
        val group = getRequestGroup(message)
        adminCheck(admin, group)
        val user = chatUserRepository.findByUsername(message.name)!!

        // Don't add admin to group
        if (user.id == admin.id) return

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
            "/topic/system/notifications/${admin.username}",
            GenericNotification(
                NotificationType.GENERIC,
                "${user.username} added to group ${group.name}"
            )
        )
    }

    @MessageMapping("/group.removeUser")
    fun removeFromGroup(@Payload message: GroupOperationMessage,
                        principal: Principal) {
        val admin = getRequestUser(principal)
        val group = getRequestGroup(message)
        adminCheck(admin, group)
        val user = chatUserRepository.findByUsername(message.name)!!

        // Don't remove admin
        if (user.id == admin.id) return

        val bindings = rabbitService.getQueueBindings(user.username)
        if(bindings.contains(group.name)) {
            // Remove binding from rabbit
            rabbitService.unbindUserFromGroup(group.name, user.username)
        }

        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GenericNotification(
                NotificationType.GENERIC,
                "Removed from group ${group.name}"
            )
        )
        messageSender.convertAndSend(
            "/topic/system/notifications/${admin.username}",
            GenericNotification(
                NotificationType.GENERIC,
                "${user.username} removed from ${group.name}"
            )
        )
    }

    @MessageMapping("/group.messages")
    fun getMessages(@Payload message: GroupOperationMessage,
                    principal: Principal) {

        val user = getRequestUser(principal)
        val group = getRequestGroup(message)
        val bindings = rabbitService.getQueueBindings(user.username)

        if(!bindings.contains(group.name)) {
            messageSender.convertAndSend(
                "/topic/system/notifications/${user.username}",
                GenericNotification(
                    NotificationType.ERROR,
                    "Not member of group ${group.name}"
                )
            )
            return
        }

        val msgList = mutableListOf<ChatMessage>()
        groupMessageRepository.findByGroup(group).forEach {
            msgList.add(gson.fromJson(it.content, ChatMessage::class.java))
        }

        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GroupMessageListNotification(
                NotificationType.MESSAGE_LIST,
                msgList
            )
        )
    }

    @MessageMapping("/group.list")
    fun getGroups(principal: Principal) {
        val user = getRequestUser(principal)
        messageSender.convertAndSend(
            "/topic/system/notifications/${user.username}",
            GroupListNotification(
                NotificationType.GROUP_LIST,
                rabbitService.getQueueBindings(user.username)
            )
        )
    }
}

