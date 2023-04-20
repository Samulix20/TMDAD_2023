package com.example.websockets.dto

enum class NotificationType {
    ERROR, GROUP_CREATED, ADDED_TO_GROUP, GROUP_LIST
}

data class GenericNotification (
    var type: NotificationType,
    var info: String
)

data class GroupListNotification (
    var type: NotificationType,
    var groups: List<String>
)
