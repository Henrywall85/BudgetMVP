package com.henry.budgetmvp.data

enum class MessageType {
    INFO, SUCCESS, ERROR, OFFLINE
}

data class StatusMessage(
    val message: String,
    val type: MessageType = MessageType.INFO
)
