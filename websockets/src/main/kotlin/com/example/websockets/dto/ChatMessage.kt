package com.example.websockets.dto

data class ChatMessage(
    var sender: String? = null,
    var toGroup: Boolean? = null,
    var receiver: String,
    var content: String,
    var timestamp: String,
    var type: MessageType
)

enum class MessageType {
    CHAT, ATTACHMENT
}
