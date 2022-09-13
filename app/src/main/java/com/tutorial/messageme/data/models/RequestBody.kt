package com.tutorial.messageme.data.models

data class RequestBody(
    val msg: String = "",
    val senderId: String = "",
    val receiverId:String = "",
    val status: Boolean = false
)