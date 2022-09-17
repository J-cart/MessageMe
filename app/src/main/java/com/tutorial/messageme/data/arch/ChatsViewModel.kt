package com.tutorial.messageme.data.arch

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.RequestBody
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(private val repository: ChatsRepository) : ViewModel() {
    private val fAuth = Firebase.auth
    private val fStoreUsers = Firebase.firestore.collection(USERS)
    private val fStoreMsg = Firebase.firestore.collection(MESSAGES)
    private val fStoreReq = Firebase.firestore.collection(FRIEND_REQUEST)

    private val _signUpState = MutableStateFlow<RequestState>(RequestState.NonExistent)
    val signUpState = _signUpState.asStateFlow()

    private val _loginState = MutableStateFlow<RequestState>(RequestState.NonExistent)
    val loginState = _loginState.asStateFlow()

    private val _allUsers = MutableStateFlow<Resource<List<UserBody>>>(Resource.Loading())
    val allUsers = _allUsers.asStateFlow()

    private val _userMessages = MutableStateFlow<Resource<List<ChatMessage>>>(Resource.Loading())
    val userMessages = _userMessages.asStateFlow()

    private val _userRequestState = MutableStateFlow<RequestState>(RequestState.Loading)
    val userRequestState = _userRequestState.asStateFlow()

    private val _sentRequestStatus = MutableStateFlow<RequestState>(RequestState.Loading)
    val sentRequestStatus = _sentRequestStatus.asStateFlow()

    private val _receivedRequestStatus = MutableStateFlow<RequestState>(RequestState.Loading)
    val receivedRequestStatus = _receivedRequestStatus.asStateFlow()

    //region NO_REPOSITORY


    fun getMessages(currentUser: FirebaseUser, otherUser: UserBody) {
        viewModelScope.launch {
            fStoreMsg.document(currentUser.uid).collection(otherUser.uid).get()
                .addOnCompleteListener { getMessages ->

                    when {
                        getMessages.isSuccessful -> {
                            val list = mutableListOf<ChatMessage>()
                            if (!getMessages.result.isEmpty) {
                                list.clear()
                                for (msg in getMessages.result.documents) {
                                    val messages = msg.toObject<ChatMessage>()
                                    messages?.let {
                                        list.add(it)
                                    }
                                }
                                Log.d("me_allMSG", "SUCCESS--->${getMessages.isComplete}")
                                _userMessages.value = Resource.Successful(list)
                                return@addOnCompleteListener
                            }
                            Log.d("me_allMSG", "ERROR--->${getMessages.exception}")
                            _userMessages.value = Resource.Failure("${getMessages.exception}")
                        }
                        else -> {
                            Log.d("me_allMSG", "ERROR--->${getMessages.exception}")
                            _userMessages.value = Resource.Failure("${getMessages.exception}")
                        }
                    }

                }
        }
    }


    fun sendMessage(currentUser: FirebaseUser, otherUser: UserBody, message: ChatMessage) {
        viewModelScope.launch {
            fStoreMsg.document(currentUser.uid).collection(otherUser.uid)
                .document(System.currentTimeMillis().toString()).set(message)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        fStoreMsg.document(otherUser.uid).collection(currentUser.uid)
                            .document(System.currentTimeMillis().toString()).set(message)
                        Log.d("me_message", "msg sent ")
                        return@addOnCompleteListener
                    }
                    Log.d("me_message", "msg NOT sent ")
                }
        }
    }

    //endregion


    //region REPOSITORY

    fun signUp(email: String, password: String) {
        _signUpState.value = RequestState.Loading
        viewModelScope.launch {
            repository.signUpNew(email, password).collect {
                when (it) {
                    is RequestState.Successful -> {
                        _signUpState.value = RequestState.Successful(it.data)
                    }
                    is RequestState.Failure -> {
                        _signUpState.value = RequestState.Failure(it.msg)
                    }
                    else -> Unit
                }

            }
        }

    }

    fun login(email: String, password: String) {
        _loginState.value = RequestState.Loading
        viewModelScope.launch {
            repository.loginUser(email, password).collect {
                when (it) {
                    is RequestState.Successful -> {
                        _loginState.value = RequestState.Successful(it.data)
                    }
                    is RequestState.Failure -> {
                        _loginState.value = RequestState.Failure(it.msg)
                    }
                    else -> Unit
                }
            }

        }
    }

    fun loadAllUsers() {

        viewModelScope.launch {
            repository.getAllUsers().collect {
                when (it) {
                    is Resource.Successful -> {
                        it.data?.let { list ->
                            _allUsers.value = Resource.Successful(list)
                        }

                    }
                    is Resource.Failure -> {
                        _allUsers.value = Resource.Failure(it.msg)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun getMsg_T(currentUser: FirebaseUser, otherUser: UserBody) {
        viewModelScope.launch {
            repository.getChatMessages(currentUser.uid, otherUser.uid).collect {
                when (it) {
                    is Resource.Successful -> {
                        _userMessages.value = Resource.Successful(it.data)
                    }
                    is Resource.Failure -> {
                        _userMessages.value = Resource.Failure(it.msg)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun sendMsg_T(currentUser: FirebaseUser, otherUser: UserBody, message: ChatMessage) {
        viewModelScope.launch {
            repository.sendMessage(currentUser.uid, otherUser.uid, message).collect {
                when (it) {
                    is RequestState.Successful -> {
                        Log.d("me_message", "msg sent ")
                    }
                    is RequestState.Failure -> {
                        Log.d("me_message", "msg NOT sent ")
                    }
                    else -> Unit
                }
            }
        }
    }

    fun loadSentRequestState(currentUserUid: String, otherUserUid: String) {
        viewModelScope.launch {
            repository.getSentRequestState(currentUserUid, otherUserUid).collect { request ->
                when (request) {
                    is RequestState.Failure -> {
                        _sentRequestStatus.value = RequestState.Failure(request.msg)
                    }
                    is RequestState.Successful -> {
                        _sentRequestStatus.value = RequestState.Successful(request.data)
                    }
                    is RequestState.NonExistent -> {
                        _sentRequestStatus.value = RequestState.NonExistent

                    }
                    else -> Unit
                }
            }
        }
    }


    fun loadReceivedRequestState(currentUserUid: String, otherUserUid: String) {
        viewModelScope.launch {
            repository.getReceivedRequestState(currentUserUid, otherUserUid).collect { request ->
                when (request) {
                    is RequestState.Failure -> {
                        _receivedRequestStatus.value = RequestState.Failure(request.msg)
                    }
                    is RequestState.Successful -> {
                        _receivedRequestStatus.value = RequestState.Successful(request.data)
                    }
                    is RequestState.NonExistent -> {
                        _receivedRequestStatus.value = RequestState.NonExistent
                    }
                    else -> Unit
                }
            }
        }

    }


    fun handleReceivedRequest(currentUser: FirebaseUser, otherUser: UserBody, state: Boolean) {
        repository.handleReceivedRequest(currentUser, otherUser, state)
    }

    fun cancelSentRequest(currentUser: FirebaseUser, otherUser: UserBody) {
        repository.cancelSentRequest(currentUser.uid, otherUser.uid)
    }


    fun sendFriendRequest(
        currentUser: FirebaseUser,
        otherUser: UserBody,
        requestBody: RequestBody
    ) {
        viewModelScope.launch {
            repository.sendFriendRequest(currentUser.uid, otherUser.uid, requestBody)
                .collect { request ->
                    when (request) {
                        is RequestState.Failure -> {
                            _userRequestState.value = RequestState.Failure(request.msg)
                        }
                        is RequestState.Successful -> {
                            _userRequestState.value = RequestState.Successful(request.data)
                        }
                        is RequestState.NonExistent -> {
                            _userRequestState.value = RequestState.NonExistent
                        }
                        else -> Unit
                    }
                }
        }
    }

    //endregion


    //region DUMPSTER
    private val _allFriendsState = MutableStateFlow<Resource<List<UserBody>>>(Resource.Loading())
    val allFriendsState = _allFriendsState.asStateFlow()

    fun getAllFriends(currentUser: FirebaseUser, otherUser: UserBody) {
        viewModelScope.launch {
            val reqList = mutableListOf<RequestBody>()
            fStoreReq.document(currentUser.uid).collection(SENT_REQUEST).get()
                .addOnCompleteListener { task ->
                    if (task.result.isEmpty || task.result != null) {
                        for (req in task.result.documents) {
                            val reqBody = req.toObject<RequestBody>()
                            reqBody?.let {
                                if (it.status) {
                                    reqList.add(it)
                                }
                            }
                        }
                    }
                }
            val friendsList = mutableListOf<UserBody>()
            repository.getAllUsers().collect {
                when (it) {
                    is Resource.Successful -> {
                        it.data?.let { users ->
                            reqList.forEach { req ->
                                users.forEach { user ->
                                    if (req.receiverId == user.uid) {
                                        friendsList.add(user)
                                    }
                                }
                            }
                        }

                    }
                    is Resource.Failure -> {
                        //show error2
                    }
                    else -> Unit
                }
            }

            /////////////////////OPTION - 2////////////////////////
            val acceptedReq = mutableListOf<String>()
            fStoreReq.document(currentUser.uid).collection(ACCEPTED_REQUEST).get()
                .addOnCompleteListener { task ->
                    if (!task.result.isEmpty || task.result != null) {
                        task.result.documents.forEach { uid ->
                            val acc = uid.toString()
                            acceptedReq.add(acc)
                        }
                    }

                }

            repository.getAllUsers().collect {
                when (it) {
                    is Resource.Successful -> {
                        it.data?.let { users ->
                            acceptedReq.forEach { req ->
                                users.forEach { user ->
                                    if (req == user.uid) {
                                        friendsList.add(user)
                                    }
                                }
                            }
                        }

                    }
                    is Resource.Failure -> {
                        //show error
                    }
                    else -> Unit
                }
            }


        }
    }


    fun addSentRequestSnapshot(currentUser: FirebaseUser, otherUser: UserBody) {
        fStoreReq.document(currentUser.uid).collection(SENT_REQUEST)
            .whereEqualTo("senderId", currentUser.uid)
            .whereEqualTo("receiverId", otherUser.uid).addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_sentRequestSnapshot", "Listen failed. $error")
                    return@addSnapshotListener
                }
                Log.d(
                    "me_sentRequestSnapshot",
                    "Listen Successful. theres the possibility of Error--> $error"
                )
                loadSentRequestState(currentUser.uid, otherUser.uid)

            }

    }

    fun addReceivedRequestSnapshot(currentUser: FirebaseUser, otherUser: UserBody) {
        fStoreReq.document(currentUser.uid).collection(RECEIVED_REQUEST)
            .whereEqualTo("senderId", otherUser.uid)
            .whereEqualTo("receiverId", currentUser.uid).addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_rec_RequestSnapshot", "Listen failed. $error")
                    return@addSnapshotListener
                }
                Log.d(
                    "me_rec_RequestSnapshot",
                    "Listen Successful. theres the possibility of Error--> $error"
                )
                loadReceivedRequestState(currentUser.uid, otherUser.uid)
            }

    }

    private val _friendsOrNot = MutableStateFlow(false)
    val friendsOrNot = _friendsOrNot.asStateFlow()
    private fun checkIfFriends(currentUser: FirebaseUser, otherUser: UserBody) {
        viewModelScope.launch {
            repository.checkIfFriends(currentUser.uid, otherUser.uid).collect {
                _friendsOrNot.value = it
            }
        }

    }


    fun loadAllFriends() {
        viewModelScope.launch {
            repository.getAllFriends().collect { resource ->
                when (resource) {
                    is Resource.Failure -> {
                        _allFriendsState.value = Resource.Failure(resource.msg)
                    }
                    is Resource.Successful -> {
                        resource.data?.let { friends ->
                            _allFriendsState.value = Resource.Successful(friends)
                        }

                    }
                    else -> Unit
                }

            }
        }
    }

    fun addAcceptedRequestSnapshot(currentUser: FirebaseUser, otherUser: UserBody) {
        fStoreReq.document(currentUser.uid).collection(ACCEPTED_REQUEST)
            .whereEqualTo("uid", otherUser.uid)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_acceptedReq", "Listen failed. $error")
                    return@addSnapshotListener
                }
                Log.d(
                    "me_acceptedReq",
                    "Listen Successful $value ::: there's the possibility of Error--> $error"
                )
                //TODO
                checkIfFriends(currentUser, otherUser)
            }

    }


    //endregion


}