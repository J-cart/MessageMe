package com.tutorial.messageme.data.arch

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.models.*
import com.tutorial.messageme.data.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(private val repository: ChatsRepository) : ViewModel() {
    private val fAuth = Firebase.auth
    private val fStoreLatestMsg = Firebase.firestore.collection(LATEST_MSG)
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

    private val _userSentRequestState = MutableStateFlow<RequestState>(RequestState.Loading)
    val userSentRequestState = _userSentRequestState.asStateFlow()

    private val _sentRequestStatus = MutableStateFlow<RequestState>(RequestState.Loading)
    val sentRequestStatus = _sentRequestStatus.asStateFlow()

    private val _receivedRequestStatus = MutableStateFlow<RequestState>(RequestState.Loading)
    val receivedRequestStatus = _receivedRequestStatus.asStateFlow()

    private val _allFriendsState = MutableStateFlow<Resource<List<UserBody>>>(Resource.Loading())
    val allFriendsState = _allFriendsState.asStateFlow()

    private val _friendsOrNot = MutableStateFlow(false)
    val friendsOrNot = _friendsOrNot.asStateFlow()

    private val _allSentReqFlow =
        MutableStateFlow<Resource<List<RequestBodyWrapper>>>(Resource.Loading())
    val allSentReqFlow = _allSentReqFlow.asStateFlow()
    private val _allRecReqFlow =
        MutableStateFlow<Resource<List<RequestBodyWrapper>>>(Resource.Loading())
    val allRecReqFlow = _allRecReqFlow.asStateFlow()



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

    private fun getMsg(currentUser: FirebaseUser, otherUser: UserBody) {
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

    fun sendMsg(
        currentUser: FirebaseUser,
        otherUser: UserBody,
        message: ChatMessage
    ) {
        viewModelScope.launch {
            repository.sendMessage(currentUser.uid, otherUser.uid, message)
                .collect {
                    when (it) {
                        is RequestState.Successful -> {
                            Log.d("me_message", "msg sent ")
                        }
                        is RequestState.Failure -> {
                            Log.d("me_message", "msg NOT sent ${it.msg} ")
                        }
                        else -> Unit
                    }
                }
        }
    }

  private  fun loadSentRequestState(currentUserUid: String, otherUserUid: String) {
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
                            _userSentRequestState.value = RequestState.Failure(request.msg)
                        }
                        is RequestState.Successful -> {
                            _userSentRequestState.value = RequestState.Successful(request.data)
                        }
                        is RequestState.NonExistent -> {
                            _userSentRequestState.value = RequestState.NonExistent
                        }
                        else -> Unit
                    }
                }
        }
    }

    //endregion


    //region DUMPSTER


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

    private fun checkIfFriends(currentUser: FirebaseUser, otherUser: UserBody) {
        viewModelScope.launch {
            repository.checkIfFriends(currentUser.uid, otherUser.uid).collect {
                _friendsOrNot.value = it
            }
        }

    }


    fun addSpecificAcceptedSnapshot(currentUser: FirebaseUser, otherUser: UserBody) {
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

    fun addAcceptedSnapshot(currentUser: FirebaseUser) {
        fStoreReq.document(currentUser.uid).collection(ACCEPTED_REQUEST)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_accepted", "Listen failed. $error")
                    return@addSnapshotListener
                }
                Log.d(
                    "me_accepted",
                    "Listen Successful $value ::: there's the possibility of Error--> $error"
                )

                loadAllFriends()
            }

    }

    fun addMessagesSnapshot(currentUser: FirebaseUser, otherUser: UserBody) {
        fStoreMsg.document(currentUser.uid).collection(otherUser.uid)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_msg", "Listen failed. $error")
                    return@addSnapshotListener
                }
                Log.d(
                    "me_msg",
                    "Listen Successful $value ::: there's the possibility of Error--> $error"
                )

                //get messages
                getMsg(currentUser, otherUser)
            }

    }

    private fun loadAllFriends() {
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


    //endregion

    private val _latestMsg = MutableStateFlow<Resource<List<LatestChatMessage>>>(Resource.Loading())
    val latestMsg = _latestMsg.asStateFlow()

    fun setLatestMsg(
        currentUser: FirebaseUser,
        otherUser: UserBody,
        latestChatMessage: LatestChatMessage
    ) {
        fStoreMsg.document(LATEST_MSG).collection(currentUser.uid).document(otherUser.uid)
            .set(latestChatMessage)
    }

    private fun getLatestMsg(currentUser: FirebaseUser) {
        viewModelScope.launch {
            repository.getLatestMsg(currentUser).collect { resource ->
                when (resource) {
                    is Resource.Successful -> {
                        _latestMsg.value = Resource.Successful(resource.data)
                    }
                    is Resource.Failure -> {
                        _latestMsg.value = Resource.Failure(resource.msg)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun addLatestMsgSnapshot(currentUser: FirebaseUser) {
        fStoreMsg.document(LATEST_MSG).collection(currentUser.uid)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_latestMsgSnapshot", "Error---->> $error")
                    return@addSnapshotListener
                }
                Log.d("me_latestMsgSnapshot", "listener success---->> $error")
                getLatestMsg(currentUser)
            }
    }

    fun addAllSentSnapshot(currentUser: FirebaseUser) {
        fStoreReq.document(currentUser.uid).collection(SENT_REQUEST)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_allSentReqSnapshot", "Error---->> $error")
                    return@addSnapshotListener
                }
                Log.d("me_allSentReqSnapshot", "listener success---->> $error")
                loadAllSentReq(currentUser)
            }
    }

    fun addAllReceivedSnapshot(currentUser: FirebaseUser) {
        fStoreReq.document(currentUser.uid).collection(RECEIVED_REQUEST)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_allRecReqSnapshot", "Error---->> $error")
                    return@addSnapshotListener
                }
                Log.d("me_allRecReqSnapshot", "listener success---->> $error")
                loadAllReceivedReq(currentUser)
            }
    }

    fun loadAllSentReq(currentUser: FirebaseUser) {
        viewModelScope.launch {
            repository.getAllSentRequest(currentUser).collect() { resource ->
                when (resource) {
                    is Resource.Successful -> {
                        _allSentReqFlow.value = Resource.Successful(resource.data)
                    }
                    is Resource.Failure -> {
                        _allSentReqFlow.value = Resource.Failure(resource.msg)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun loadAllReceivedReq(currentUser: FirebaseUser) {
        viewModelScope.launch {
            repository.getAllReceivedRequest(currentUser).collect() { resource ->
                when (resource) {
                    is Resource.Successful -> {
                        _allRecReqFlow.value = Resource.Successful(resource.data)
                    }
                    is Resource.Failure -> {
                        _allRecReqFlow.value = Resource.Failure(resource.msg)
                    }
                    else -> Unit
                }
            }
        }
    }

}