package com.example.websockets.chat

import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.security.Principal

@Service
class UserConnections {
    var hashMap : HashMap<String, Pair<String, String>> = HashMap()

    fun addUser(id : String, name : String, group: String) {
        hashMap[id] = Pair(name, group)
    }

    fun deleteUser(id : String) : Pair<String, String> {
        val p = hashMap[id]
        hashMap.remove(id)
        return p!!
    }
}

@Controller
class ChatController (
    val userData : UserConnections,
    val messageSender: SimpMessageSendingOperations,
) {
    @MessageMapping("/chat.register")
    fun register(@Payload chatMessage: ChatMessage,
                 headerAccessor: SimpMessageHeaderAccessor,
                 principal: Principal) {
        userData.addUser(headerAccessor.sessionId!!, chatMessage.sender, chatMessage.group)
        messageSender.convertAndSend(
            "/topic/${chatMessage.group}",
            chatMessage
        )
    }

    @MessageMapping("/chat.send")
    fun chat(@Payload chatMessage: ChatMessage, headerAccessor: SimpMessageHeaderAccessor) {
        messageSender.convertAndSend("/topic/${chatMessage.group}", chatMessage)
    }
}

@Component
class WebSocketEventListener (
    val userData: UserConnections,
    val messageSender: SimpMessageSendingOperations
) {
    @EventListener
    fun handleSessionDisconnect(event : SessionDisconnectEvent) {
        val p = userData.deleteUser(event.sessionId)
        messageSender.convertAndSend(
            "/topic/${p.second}",
            ChatMessage(null, p.first, p.second, MessageType.LEAVE)
        )
    }
}
