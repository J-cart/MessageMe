package com.tutorial.messageme.data.models

data class FriendRequest(
    val msg: String = "",
    val senderId: String = "",
    val status: Boolean = false
)