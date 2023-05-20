package com.example.websockets.dto

enum class NotificationType {
    ERROR, WRITE_FILE_ERROR,
    GENERIC, GROUP_LIST, UPLOAD_FILE, MESSAGE_LIST
}

data class GenericNotification (
    var type: NotificationType,
    var info: String
)

data class GroupListNotification (
    var type: NotificationType,
    var groups: List<String>
)

data class GroupMessageListNotification (
    var type: NotificationType,
    var messages: List<ChatMessage>
)

data class UploadFileNotification (
    var type: NotificationType,
    var url: String,
    var uuid: String
)
