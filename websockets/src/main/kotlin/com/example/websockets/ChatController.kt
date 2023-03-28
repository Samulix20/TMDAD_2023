package com.example.websockets

import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Service
class UserConnections {
    var hashMap : HashMap<String, String> = HashMap()

    fun addUser(id : String, name : String) {
        hashMap[id] = name
    }

    fun deleteUser(id : String) : String? {
        val name : String? = hashMap[id]
        hashMap.remove(id)
        return name
    }
}

@Controller
class ChatController (
    val userData : UserConnections
) {
    @MessageMapping("/chat.register")
    @SendTo("/topic/public")
    fun register(@Payload chatMessage: ChatMessage,
                 headerAccessor: SimpMessageHeaderAccessor,
                 @Header("simpSessionId") sessionId : String) : ChatMessage {
        userData.addUser(sessionId, chatMessage.sender)
        return chatMessage
    }

    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    fun chat(@Payload chatMessage: ChatMessage, headerAccessor: SimpMessageHeaderAccessor) : ChatMessage {
        return chatMessage
    }
}

@Component
class WebSocketEventListener (
    val userData: UserConnections,
    val messageSender: SimpMessageSendingOperations
) {
    @EventListener
    fun handleSessionDisconnect(event : SessionDisconnectEvent) {
        val name = userData.deleteUser(event.sessionId)
        if(name != null) {
            messageSender.convertAndSend(
                "/topic/public",
                ChatMessage(null, name, MessageType.LEAVE))
        }
    }
}