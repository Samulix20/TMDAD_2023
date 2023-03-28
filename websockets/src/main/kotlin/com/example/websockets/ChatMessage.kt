package com.example.websockets

data class ChatMessage(
    var content: String? = null,
    var sender: String,
    var type: MessageType
)

enum class MessageType {
    CHAT, LEAVE, JOIN
}
