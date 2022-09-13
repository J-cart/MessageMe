package com.tutorial.messageme.data.arch

import com.google.firebase.auth.FirebaseUser
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.RequestBody
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.RequestState
import com.tutorial.messageme.data.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ChatsRepository {
fun signUpNew(email:String,password:String):Flow<RequestState>
fun loginUser(email: String,password: String):Flow<RequestState>
fun sendFriendRequest(currentUser: String,otherUser:UserBody,requestBody: RequestBody):Flow<RequestState>
fun checkFriendRequest(currentUserUid:String,otherUserUid: String):Flow<Boolean>
fun getChatMessages(currentUser: String,otherUser: UserBody):Flow<Resource<List<ChatMessage>>>
fun getAllUsers():Flow<Resource<List<UserBody>>>
fun sendMessage(currentUser: String,otherUser: UserBody,message: ChatMessage):Flow<RequestState>

}