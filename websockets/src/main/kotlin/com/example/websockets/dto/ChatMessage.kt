package com.example.websockets.dto

import javax.sound.midi.MidiDeviceReceiver

data class ChatMessage(
    var content: String? = null,
    var sender: String? = null,
    var receiver: String,
    var type: MessageType
)

enum class MessageType {
    CHAT, LEAVE, JOIN
}
