package com.example.websockets.dto

data class ChatMessage(
    var content: String? = null,
    var sender: String,
    var group: String,
    var type: MessageType
)

enum class MessageType {
    CHAT, LEAVE, JOIN
}
