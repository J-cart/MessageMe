package com.tutorial.messageme.data.arch

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.RequestBody
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
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

    private val _requestStatus = MutableStateFlow<RequestState>(RequestState.NonExistent)
    val requestStatus = _requestStatus.asStateFlow()

    //region NO_REPOSITORY
    fun signUpNewUser(email: String, password: String) {
        _signUpState.value = RequestState.Loading
        viewModelScope.launch {
            fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { signUp ->
                when {
                    signUp.isSuccessful -> {
                        Log.d("me_signUp", "SUCCESS--->${signUp.isComplete}")
                        val newUser = signUp.result.user?.uid?.let {
                            UserBody(email = email, uid = it)
                        } ?: UserBody(email = email)

                        fStoreUsers.document(email).set(newUser).addOnCompleteListener { addUser ->
                            if (addUser.isSuccessful) {
                                Log.d("me_addUsers", "SUCCESS--->${addUser.isComplete}")
                                _signUpState.value =
                                    RequestState.Successful(addUser.isComplete && signUp.isComplete)
                            } else {
                                Log.d("me_addUsers", "ERROR--->${addUser.exception}")
                                addUser.exception?.message?.let {
                                    _signUpState.value = RequestState.Failure(it)
                                } ?: RequestState.Failure("Unable to save user data...")
                            }

                        }
                    }
                    else -> {
                        Log.d("me_signUp", "ERROR--->${signUp.exception}")
                        signUp.exception?.message?.let {
                            _signUpState.value = RequestState.Failure(it)
                        } ?: RequestState.Failure("Error While Signing Up...")

                    }
                }
            }
        }
    }

    fun loginUser(email: String, password: String) {
        _loginState.value = RequestState.Loading
        viewModelScope.launch {
            fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { login ->

                if (login.isSuccessful) {
                    Log.d("me_login", "SUCCESS--->${login.isComplete}")
                    _loginState.value = RequestState.Successful(login.isComplete)
                } else {
                    login.exception?.message?.let {
                        Log.d("me_login", "ERROR--->${login.exception}")
                        _loginState.value = RequestState.Failure(it)
                    } ?: RequestState.Failure("Error While Signing In...")

                }
            }
        }
    }

    fun getAllUsers() {
        //_allUsers.value = Resource.Loading()
        viewModelScope.launch {
            fStoreUsers.get().addOnCompleteListener { getUsers ->

                when {
                    getUsers.isSuccessful -> {
                        val list = mutableListOf<UserBody>()
                        if (!getUsers.result.isEmpty) {
                            list.clear()
                            for (docs in getUsers.result.documents) {
                                val user = docs.toObject<UserBody>()
                                user?.let {
                                    if (it.email != fAuth.currentUser?.email) {
                                        list.add(it)
                                    }
                                }
                            }
                            Log.d("me_users", "SUCCESS--->$list")
                            _allUsers.value = Resource.Successful(list)
                            return@addOnCompleteListener
                        }
                        Log.d("me_users", "ERROR--->${getUsers.exception}")
                        _allUsers.value = Resource.Failure(getUsers.exception.toString())
                    }
                    else -> {
                        Log.d("me_users", "ERROR--->${getUsers.exception}")
                        _allUsers.value = Resource.Failure(getUsers.exception.toString())
                    }
                }

            }
        }

    }

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

    fun sendFriendRequest(
        currentUser: FirebaseUser,
        otherUser: UserBody,
        requestBody: RequestBody
    ) {

        //TODO : figure out the logic behind how to send the req without overwriting any of the existing req from same user
        val senderId = currentUser.uid + otherUser.uid
        val receiverId = otherUser.uid + currentUser.uid

        viewModelScope.launch {
            checkRequest(currentUser, otherUser).collect { exists ->
                when {
                    exists -> {
                        Log.d("me_req", "req sent not sent..it exists already ")
                        _userRequestState.value = RequestState.Failure("Request Exists Already")
                    }
                    else -> {
                        fStoreReq.document(currentUser.uid).collection("requests")
                            .document(senderId)
                            .set(requestBody).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    fStoreReq.document(otherUser.uid).collection("requests")
                                        .document(receiverId)
                                        .set(requestBody)
                                    _userRequestState.value = RequestState.Successful(true)
                                    Log.d("me_req", "req sent ")
                                } else {
                                    _userRequestState.value =
                                        RequestState.Failure(it.exception.toString())
                                    Log.d("me_req", "req not sent ${it.exception.toString()} ")
                                }
                            }
                    }
                }
            }
        }

    }


    private fun checkRequest(currentUser: FirebaseUser, otherUser: UserBody): Flow<Boolean> {
        return callbackFlow {
            fStoreReq.document(currentUser.uid).collection(SENT_REQUEST)
                .whereEqualTo("senderId", currentUser.uid)
                .whereEqualTo("receiverId", otherUser.uid)
                .get().addOnCompleteListener { req ->
                    when{
                        req.isSuccessful->{
                            if (!req.result.isEmpty || req.result != null) {
                                trySend(true)
                                Log.d("me_checkIfReqExists", true.toString())
                                return@addOnCompleteListener
                            }
                            trySend(false)
                            Log.d("me_checkIfReqExists", false.toString())

                        }
                        else->{
                            trySend(false)
                            Log.d("me_checkIfReqExists", "${req.exception}")
                        }
                    }


                }
            awaitClose()
        }
    }

    fun getRequestState(currentUser: FirebaseUser, otherUser: UserBody) {
        viewModelScope.launch {
            checkRequest(currentUser, otherUser).collect { exists ->
                when {
                    exists -> {
                        fStoreReq.document(currentUser.uid).collection(SENT_REQUEST)
                            .whereEqualTo("senderId", currentUser.uid)
                            .whereEqualTo("receiverId", otherUser.uid)
                            .get().addOnCompleteListener { req ->
                                if (req.isComplete) {
                                    val requestBody =
                                        req.result.documents[0].toObject<RequestBody>()
                                    requestBody?.status?.let {
                                        _requestStatus.value = RequestState.Successful(it)
                                        Log.d("me_checkRequestState", "$requestBody")
                                    } ?: RequestState.Successful(false)

                                    return@addOnCompleteListener
                                }
                                Log.d("me_checkRequestState", "${req.exception}")
                                _requestStatus.value =
                                    RequestState.Failure("An Error Occurred ${req.exception}")

                            }
                    }
                    else -> {
                        Log.d("me_checkRequestState", "NON_EXISTENT")
                        _requestStatus.value = RequestState.NonExistent
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

    fun getUsers() {
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

    fun getMsg(currentUser: FirebaseUser, otherUser: UserBody) {
        viewModelScope.launch {
            repository.getChatMessages(currentUser.uid, otherUser).collect {
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

    fun sendReq(currentUser: FirebaseUser, otherUser: UserBody, requestBody: RequestBody) {
        viewModelScope.launch {
            repository.sendFriendRequest(currentUser.uid, otherUser, requestBody).collect {
                when (it) {
                    is RequestState.Successful -> {
                        _userRequestState.value = RequestState.Successful(it.data)
                    }
                    is RequestState.Failure -> {
                        _userRequestState.value = RequestState.Failure(it.msg)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun sendMsg(currentUser: FirebaseUser, otherUser: UserBody, message: ChatMessage) {
        viewModelScope.launch {
            repository.sendMessage(currentUser.uid, otherUser, message).collect {
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


    //endregion


    fun sender(
        currentUser: FirebaseUser,
        otherUser: UserBody,
        requestBody: RequestBody
    ) {

        //TODO : figure out the logic behind how to send the req without overwriting any of the existing req from same user
        val requestId = currentUser.uid + otherUser.uid

        viewModelScope.launch {
            checkRequest(currentUser, otherUser).collect { exists ->
                when {
                    exists -> {
                        Log.d("me_req", "req sent not sent..it exists already ")
                        _userRequestState.value = RequestState.Failure("Request Exists Already")
                    }
                    else -> {
                        fStoreReq.document(currentUser.uid).collection(SENT_REQUEST)
                            .document(requestId)
                            .set(requestBody).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    fStoreReq.document(otherUser.uid).collection(RECEIVED_REQUEST)
                                        .document(requestId)
                                        .set(requestBody)
                                    _userRequestState.value = RequestState.Successful(true)
                                    Log.d("me_req", "req sent ")
                                } else {
                                    _userRequestState.value =
                                        RequestState.Failure(it.exception.toString())
                                    Log.d("me_req", "req not sent ${it.exception.toString()} ")
                                }
                            }
                    }
                }
            }
        }

    }
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
                            acceptedReq.forEach { req->
                                users.forEach { user->
                                    if (req == user.uid){
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

    fun handleRequest(currentUser: FirebaseUser,otherUser: UserBody,state:Boolean){
        viewModelScope.launch {
            val requestId = currentUser.uid + otherUser.uid

            if (state){
                //Accepted
                fStoreReq.document(currentUser.uid).collection(RECEIVED_REQUEST).document(requestId).update(
                    "status",state
                ).addOnCompleteListener {
                    if (it.isSuccessful){
                        val user = UserBody(
                            uid = otherUser.uid,
                            userName = otherUser.userName,
                            fName = otherUser.fName,
                            lName = otherUser.lName,
                            phoneNo = otherUser.phoneNo,
                            email = otherUser.email,
                            displayImg = otherUser.displayImg,
                            userStatus = otherUser.userStatus,
                            dob = otherUser.dob,
                            gender = otherUser.gender
                        )
                        fStoreReq.document(currentUser.uid).collection(ACCEPTED_REQUEST).document(otherUser.uid).set(user)
                        fStoreReq.document(otherUser.uid).collection(SENT_REQUEST).document(requestId).update(
                            "status",state
                        )
                    }
                }

            }else{
                //Decline and delete
                fStoreReq.document(currentUser.uid).collection(RECEIVED_REQUEST).document(requestId).delete().addOnCompleteListener {
                    fStoreReq.document(otherUser.uid).collection(SENT_REQUEST).document(requestId).delete()
                }

            }


        }
    }

    fun addRequestSnapshot(currentUser: FirebaseUser,otherUser:UserBody){
        fStoreReq.document(currentUser.uid).collection(SENT_REQUEST)
            .whereEqualTo("senderId", currentUser.uid)
            .whereEqualTo("receiverId", otherUser.uid).addSnapshotListener { value, error ->
                if (error != null) {
                    Log.w("me_requestSnapshot", "Listen failed. $error" )
                    return@addSnapshotListener
                }
                getRequestState(currentUser, otherUser)

            }

    }



}