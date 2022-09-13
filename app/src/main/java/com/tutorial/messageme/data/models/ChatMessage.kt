package com.tutorial.messageme.data.models

data class ChatMessage(
    val status: Boolean = false,
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val messageType: String = "",
    val timeStamp: String = ""
)