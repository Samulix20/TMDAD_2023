package com.example.websockets.dto

enum class NotificationType {
    ERROR, WRITE_FILE_ERROR,
    GENERIC, GROUP_LIST, UPLOAD_FILE
}

data class GenericNotification (
    var type: NotificationType,
    var info: String
)

data class GroupListNotification (
    var type: NotificationType,
    var groups: List<String>
)

data class UploadFileNotification (
    var type: NotificationType,
    var url: String,
    var uuid: String
)
