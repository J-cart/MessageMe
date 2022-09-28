package com.tutorial.messageme.data.arch

import com.google.firebase.auth.FirebaseUser
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.LatestChatMessage
import com.tutorial.messageme.data.models.RequestBody
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.RequestState
import com.tutorial.messageme.data.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ChatsRepository {
    fun signUpNew(email: String, password: String): Flow<RequestState>
    fun loginUser(email: String, password: String): Flow<RequestState>
    fun sendFriendRequest(
        currentUserUid: String,
        otherUserUid: String,
        requestBody: RequestBody
    ): Flow<RequestState>

    fun getChatMessages(currentUserUid: String, otherUserUid: String): Flow<Resource<List<ChatMessage>>>
    fun getAllUsers(): Flow<Resource<List<UserBody>>>
    fun sendMessage(
        currentUserUid: String,
        otherUserUid: String,
        message: ChatMessage
    ): Flow<RequestState>


    fun getSentRequestState(currentUserUid: String, otherUserUid: String): Flow<RequestState>
    fun checkSentRequest(currentUserUid: String, otherUserUid: String): Flow<Boolean>

    fun getReceivedRequestState(currentUserUid: String, otherUserUid: String): Flow<RequestState>
    fun checkReceivedRequest(currentUserUid: String, otherUserUid: String): Flow<Boolean>
    fun handleReceivedRequest(currentUser: FirebaseUser, otherUser: UserBody, state:Boolean)

    fun cancelSentRequest(currentUserUid: String, otherUserUid: String)

    fun checkIfFriends(currentUserUid: String, otherUserUid: String): Flow<Boolean>

    fun getAllFriends(): Flow<Resource<List<UserBody>>>

    fun getLatestMsg(currentUser: FirebaseUser): Flow<Resource<List<LatestChatMessage>>>

}