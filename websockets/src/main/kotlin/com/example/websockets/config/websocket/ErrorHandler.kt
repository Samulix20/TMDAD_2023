package com.example.websockets.config.websocket

import org.springframework.messaging.Message
import org.springframework.messaging.simp.stomp.DefaultStompSession
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler

// Used for sending custom error messages on exceptions during
// websocket sessions
class ErrorHandler : StompSubProtocolErrorHandler() {
    override fun handleClientMessageProcessingError(
        clientMessage: Message<ByteArray>?,
        ex: Throwable
    ): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(StompCommand.ERROR)
        accessor.message = ex.cause?.message ?: ex.message
        accessor.setLeaveMutable(true)
        return MessageBuilder.createMessage(DefaultStompSession.EMPTY_PAYLOAD, accessor.messageHeaders)
    }
}