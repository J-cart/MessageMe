package com.tutorial.messageme.data.utils

import android.content.Context
import android.widget.Toast

const val SENT_REQUEST = "sent_request"
const val  RECEIVED_REQUEST = "received_request"
const val ACCEPTED_REQUEST = "accepted_request"
const val USERS = "Users"
const val MESSAGES = "Messages"
const val FRIEND_REQUEST = "Friend_Requests"
const val SENDER = 1
const val RECEIVER = 2
const val TYPE_TEXT = "text"
const val TYPE_IMG = "image"
const val TYPE_AUDIO = "audio"
const val SENDER_ID = "senderId"
const val RECEIVER_ID = "receiverId"
const val LATEST_MSG = "Latest_Messages"
const val WEB_KEY = "key =AAAATZtXHlE:APA91bEg9oInCbg0m_rkJQzBDK3zvsjN4uh6e5Zvm9wQofCoYTvMooz6NeSp9EMaknV3e7T275-BKRDV9S1Xcr5eGsMFGScsQDlGYoA28Rtxgmn75sHiiMGgI6to7z0T-nODxWIRB2WT"
const val FCM_URL = "https://fcm.googleapis.com/fcm/send"
const val VIEW_CHAT = "Chat Fragment"

fun Context.showToast(text:String){
    Toast.makeText(this,text,Toast.LENGTH_SHORT).show()
}