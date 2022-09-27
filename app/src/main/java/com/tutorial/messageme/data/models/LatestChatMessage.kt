package com.tutorial.messageme.data.models

data class LatestChatMessage(
    val userBody: UserBody = UserBody(),
    val chatMessage: ChatMessage = ChatMessage()
)