package com.tutorial.messageme.data.arch

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.tutorial.messageme.data.models.*
import com.tutorial.messageme.data.utils.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatsRepositoryImpl : ChatsRepository {

    private val fAuth = Firebase.auth
    private val fStoreUsers = Firebase.firestore.collection(USERS)
    private val fStoreMsg = Firebase.firestore.collection(MESSAGES)
    private val fStoreReq = Firebase.firestore.collection(FRIEND_REQUEST)
    private val _tokenUpdateFlow = MutableStateFlow<RequestState>(RequestState.NonExistent)
    override val tokenUpdateFlow: StateFlow<RequestState>
        get() = _tokenUpdateFlow.asStateFlow()

    override fun signUpNew(email: String, password: String): Flow<RequestState> {
        return callbackFlow {
            try {
                val tokenReg = FirebaseMessaging.getInstance().token.await()
                val tokenList = listOf(tokenReg)
                val signUp = fAuth.createUserWithEmailAndPassword(email, password).await()
                val newUser = signUp.user?.uid?.let {
                    UserBody(email = email, uid = it, deviceToken = tokenList)
                } ?: UserBody(email = email, deviceToken = tokenList)
                fStoreUsers.document(email).set(newUser).await()
                trySend(RequestState.Successful(true))
                Log.d("me_addUsers", "SUCCESS ALl TRANSACTION COMPLETED")
            } catch (e: Exception) {
                trySend(RequestState.Failure("$e"))
                Log.d("me_addUsers", "ERROR--->$e")
            }
            awaitClose()
        }

    }

    override fun loginUser(email: String, password: String): Flow<RequestState> = callbackFlow {
        fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { login ->

            if (login.isSuccessful) {
                Log.d("me_login", "SUCCESS--->${login.isComplete}")
                trySend(RequestState.Successful(login.isComplete))
            } else {
                login.exception?.message?.let {
                    Log.d("me_login", "ERROR--->${login.exception}")
                    trySend(RequestState.Failure(it))
                } ?: RequestState.Failure("Error While Signing In...")

            }
        }
        awaitClose()
    }

    override fun sendFriendRequest(
        currentUserUid: String,
        otherUserUid: String,
        requestBody: RequestBody
    ): Flow<RequestState> {
        return callbackFlow {
            val requestId = currentUserUid + otherUserUid

            checkSentRequest(currentUserUid, otherUserUid).collect { exists ->
                when {
                    exists -> {
                        Log.d("me_req", "req sent not sent..it exists already ")
                        trySend(RequestState.Failure("Request Exists Already"))
                    }
                    else -> {
                        fStoreReq.document(currentUserUid).collection(SENT_REQUEST)
                            .document(requestId)
                            .set(requestBody).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    fStoreReq.document(otherUserUid).collection(RECEIVED_REQUEST)
                                        .document(requestId)
                                        .set(requestBody)
                                    trySend(RequestState.Successful(true))
                                    Log.d("me_req", "req sent ")
                                } else {
                                    trySend(RequestState.Failure(it.exception.toString()))

                                    Log.d("me_req", "req not sent ${it.exception.toString()} ")
                                }
                            }
                    }
                }
            }


            awaitClose()
        }
    }


    override fun getChatMessages(
        currentUserUid: String,
        otherUserUid: String
    ): Flow<Resource<List<ChatMessage>>> = callbackFlow {
        fStoreMsg.document(currentUserUid).collection(otherUserUid).get()
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
                            trySend(Resource.Successful(list))
                            return@addOnCompleteListener
                        }
                        Log.d("me_allMSG", "ERROR--->${getMessages.exception}")
                        trySend(Resource.Failure("${getMessages.exception}"))
                    }
                    else -> {
                        Log.d("me_allMSG", "ERROR--->${getMessages.exception}")
                        trySend(Resource.Failure("${getMessages.exception}"))
                    }
                }

            }
        awaitClose()
    }

    override fun getAllUsers(): Flow<Resource<List<UserBody>>> = callbackFlow {
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
                        trySend(Resource.Successful(list))
                        return@addOnCompleteListener
                    }
                    Log.d("me_users", "ERROR--->${getUsers.exception}")
                    trySend(Resource.Failure(getUsers.exception.toString()))
                }
                else -> {
                    Log.d("me_users", "ERROR--->${getUsers.exception}")
                    trySend(Resource.Failure(getUsers.exception.toString()))
                }
            }

        }
        awaitClose()
    }

    override fun sendMessage(
        currentUserUid: String,
        otherUser: UserBody,
        message: ChatMessage
    ): Flow<RequestState> {
        return callbackFlow {

            val curMsgRef = fStoreMsg.document(currentUserUid).collection(otherUser.uid)
                .document(System.currentTimeMillis().toString())
            val otherMsgRef = fStoreMsg.document(otherUser.uid).collection(currentUserUid)
                .document(System.currentTimeMillis().toString())
            val lstMsgRefCur =
                fStoreMsg.document(LATEST_MSG).collection(currentUserUid).document(otherUser.uid)
            val lstMsgRefOther =
                fStoreMsg.document(LATEST_MSG).collection(otherUser.uid).document(currentUserUid)
            val curMsgBody = LatestMsgWrapper(otherUser.uid, message)
            val otherMsgBody = LatestMsgWrapper(currentUserUid, message)
            Firebase.firestore.runBatch { batch ->
                batch.set(curMsgRef, message)
                batch.set(otherMsgRef, message)
                batch.set(lstMsgRefCur, curMsgBody)
                batch.set(lstMsgRefOther, otherMsgBody)
                someMoreStuffs(message, otherUser)
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(RequestState.Successful(task.isComplete))
                    Log.d("me_message", "msg sent ")
                    return@addOnCompleteListener
                }
                Log.d("me_message", "msg NOT sent -- ${task.exception} ")
                trySend(RequestState.Failure(task.exception.toString()))
            }

            awaitClose()
        }
    }

    override fun checkSentRequest(currentUserUid: String, otherUserUid: String): Flow<Boolean> =
        callbackFlow {
            fStoreReq.document(currentUserUid).collection(SENT_REQUEST)
                .whereEqualTo(SENDER_ID, currentUserUid)
                .whereEqualTo(RECEIVER_ID, otherUserUid)
                .get().addOnCompleteListener { req ->
                    when {
                        req.isSuccessful -> {
                            if (!req.result.isEmpty) {
                                trySend(true)
                                Log.d("me_checkIfReqExists", true.toString())
                                return@addOnCompleteListener
                            }
                            trySend(false)
                            Log.d("me_checkIfReqExists", false.toString())

                        }
                        else -> {
                            trySend(false)
                            Log.d("me_checkIfReqExists", "${req.exception}")
                        }
                    }


                }
            awaitClose()
        }

    override fun getSentRequestState(
        currentUserUid: String,
        otherUserUid: String
    ): Flow<RequestState> {
        return callbackFlow {
            checkSentRequest(currentUserUid, otherUserUid).collect { exists ->
                when {
                    exists -> {
                        fStoreReq.document(currentUserUid).collection(SENT_REQUEST)
                            .whereEqualTo(SENDER_ID, currentUserUid)
                            .whereEqualTo(RECEIVER_ID, otherUserUid)
                            .get().addOnCompleteListener { req ->

                                if (req.isSuccessful) {
                                    try {
                                        val request =
                                            req.result.documents[0].toObject<RequestBody>()
                                        request?.let {
                                            trySend(RequestState.Successful(it.status))
                                        } ?: trySend(RequestState.Successful(false))
                                        Log.d("me_checkRequestState", "reqList -->$request")

                                    } catch (e: Exception) {
                                        Log.d("me_checkRequestState", "exception -->$e")
                                    }

                                    return@addOnCompleteListener
                                }

                                Log.d("me_checkRequestState", "${req.exception}")
                                trySend(RequestState.Failure("An Error Occurred ${req.exception}"))


                            }
                    }
                    else -> {
                        Log.d("me_checkRequestState", "NON_EXISTENT")
                        trySend(RequestState.NonExistent)
                    }
                }

            }
            awaitClose()
        }
    }


    override fun getReceivedRequestState(
        currentUserUid: String,
        otherUserUid: String
    ): Flow<RequestState> {
        return callbackFlow {
            checkReceivedRequest(currentUserUid, otherUserUid).collect { exists ->
                when {
                    exists -> {
                        fStoreReq.document(currentUserUid).collection(RECEIVED_REQUEST)
                            .whereEqualTo(SENDER_ID, otherUserUid)
                            .whereEqualTo(RECEIVER_ID, currentUserUid)
                            .get().addOnCompleteListener { req ->

                                if (req.isSuccessful) {
                                    try {
                                        val request =
                                            req.result.documents[0].toObject<RequestBody>()
                                        request?.let {
                                            trySend(RequestState.Successful(it.status))
                                        } ?: trySend(RequestState.Successful(false))
                                        Log.d(
                                            "me_receivedRequestState",
                                            "receivedReqList -->$request"
                                        )

                                    } catch (e: Exception) {
                                        Log.d("me_receivedRequestState", "exception -->$e")
                                    }

                                    return@addOnCompleteListener
                                }

                                Log.d("me_receivedRequestState", "${req.exception}")
                                trySend(RequestState.Failure("An Error Occurred ${req.exception}"))


                            }
                    }
                    else -> {
                        Log.d("me_receivedRequestState", "NON_EXISTENT")
                        trySend(RequestState.NonExistent)
                    }
                }

            }
            awaitClose()
        }
    }

    override fun checkReceivedRequest(currentUserUid: String, otherUserUid: String): Flow<Boolean> =
        callbackFlow {
            fStoreReq.document(currentUserUid).collection(RECEIVED_REQUEST)
                .whereEqualTo(SENDER_ID, otherUserUid)
                .whereEqualTo(RECEIVER_ID, currentUserUid)
                .get().addOnCompleteListener { req ->
                    when {
                        req.isSuccessful -> {
                            if (!req.result.isEmpty) {
                                trySend(true)
                                Log.d("me_Rec_checkIfReqExists", true.toString())
                                return@addOnCompleteListener
                            }
                            trySend(false)
                            Log.d("me_Rec_checkIfReqExists", false.toString())
                        }
                        else -> {
                            trySend(false)
                            Log.d("me_Rec_checkIfReqExists", "${req.exception}")
                        }
                    }
                }
            awaitClose()
        }


    override fun handleReceivedRequest(
        currentUser: FirebaseUser,
        otherUser: UserBody,
        state: Boolean
    ) {
        val requestId = otherUser.uid + currentUser.uid

        if (state) {
            //Accepted
            fStoreReq.document(currentUser.uid).collection(RECEIVED_REQUEST).document(requestId)
                .update(
                    "status", state
                ).addOnCompleteListener {
                    if (it.isSuccessful) {


                        val user =
                            currentUser.email?.let { it1 ->
                                UserBody(
                                    uid = currentUser.uid,
                                    userName = "currentUser.userName",
                                    fName = "currentUser.fName",
                                    lName = "currentUser.lName",
                                    phoneNo = "currentUser.phoneNo",
                                    email = it1,
                                    displayImg = "currentUser.displayImg",
                                    userStatus = "currentUser.userStatus",
                                    dob = "currentUser.dob",
                                    gender = "currentUser.gender"
                                )
                            } ?: "dummy@gmail.com"


                        fStoreReq.document(otherUser.uid).collection(SENT_REQUEST)
                            .document(requestId).update(
                                "status", state
                            )
                        fStoreReq.document(currentUser.uid).collection(ACCEPTED_REQUEST)
                            .document(otherUser.uid).set(otherUser)
                        fStoreReq.document(otherUser.uid).collection(ACCEPTED_REQUEST)
                            .document(currentUser.uid).set(user)
                    }
                }

        } else {
            //Decline and delete
            fStoreReq.document(currentUser.uid).collection(RECEIVED_REQUEST).document(requestId)
                .delete().addOnCompleteListener {
                    fStoreReq.document(otherUser.uid).collection(SENT_REQUEST)
                        .document(requestId)
                        .delete()
                }
        }
    }

    override fun cancelSentRequest(currentUserUid: String, otherUserUid: String) {
        val requestId = currentUserUid + otherUserUid

        fStoreReq.document(currentUserUid).collection(SENT_REQUEST)
            .document(requestId).delete()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    fStoreReq.document(otherUserUid).collection(RECEIVED_REQUEST)
                        .document(requestId).delete()
                    Log.d("me_reqCancel", "req canceled ")
                } else {
                    Log.d("me_reqCancel", "req not canceled ${it.exception.toString()} ")
                }
            }
    }


    override fun checkIfFriends(currentUserUid: String, otherUserUid: String): Flow<Boolean> {
        return callbackFlow {
            fStoreReq.document(currentUserUid).collection(ACCEPTED_REQUEST)
                .document(otherUserUid).get().addOnCompleteListener {
                    if (it.result.exists()) {
                        trySend(true)
                        return@addOnCompleteListener
                    }
                    trySend(false)
                }
            awaitClose()
        }
    }

    override fun getAllFriends(): Flow<Resource<List<UserBody>>> {
        return callbackFlow {
            getAllUsers().collect { resource ->
                when (resource) {
                    is Resource.Failure -> {
                        trySend(Resource.Failure(resource.msg))
                    }
                    is Resource.Successful -> {
                        fAuth.currentUser?.let { currentUser ->
                            try {
                                val friendsList = mutableListOf<UserBody>()
                                val collection =
                                    fStoreReq.document(currentUser.uid).collection(ACCEPTED_REQUEST)
                                        .get().await()
                                friendsList.clear()
                                collection.documents.forEach { user ->
                                    Log.d("me_friendListTest", "in collection")
                                    user.toObject<UserBody>()?.let {
                                        resource.data?.let { userList ->
                                            userList.forEach { mainUser ->
                                                if (it.uid == mainUser.uid) {
                                                    friendsList.add(mainUser)
                                                }
                                            }
                                        }
                                    }
                                }
                                trySend(Resource.Successful(friendsList))
                                Log.d("me_friendListTest", "success==>$friendsList")
                            } catch (e: Exception) {
                                trySend(Resource.Failure(e.toString()))
                                Log.d("me_friendListTest", "exception ---$e")
                            }
                        }

                    }
                    else -> Unit
                }
            }
        }

    }

    override fun getLatestMsg(currentUser: FirebaseUser): Flow<Resource<List<LatestChatMessage>>> {

        return callbackFlow {
            try {

                val msgList = mutableListOf<LatestChatMessage>()
                val collection =
                    fStoreMsg.document(LATEST_MSG).collection(currentUser.uid).get().await()
                collection.documents.forEach { message ->
                    Log.d("me_latestMsgTest", "in collection")
                    message.toObject<LatestMsgWrapper>()?.let { latestMsg ->
                        val newTask = fStoreUsers.whereEqualTo("uid", latestMsg.uid)
                            .get().await()
                        Log.d("me_latestMsgTest", "in newUser")
                        val userBody = newTask.documents[0].toObject<UserBody>()
                        userBody?.let {
                            msgList.add(LatestChatMessage(it, latestMsg.chatMessage))
                        }
                    }

                }
                Log.d("me_latestMsgTest", "success ===> $msgList")
                trySend(Resource.Successful(msgList))
            } catch (e: Exception) {
                Log.d("me_latestMsgTest", "exception ---$e")
            }
            awaitClose()
        }

    }

    override fun getAllSentRequest(currentUser: FirebaseUser): Flow<Resource<List<RequestBodyWrapper>>> {

        return callbackFlow {
            try {
                val list = mutableListOf<RequestBodyWrapper>()
                val sentReq =
                    fStoreReq.document(currentUser.uid).collection(SENT_REQUEST).get().await()
                Log.d("me_getRecReq", "all sentReq == ${sentReq.documents}")
                sentReq.documents.forEach { request ->
                    request.toObject<RequestBody>()?.let { reqBody ->
                        val newTask = fStoreUsers.whereEqualTo("uid", reqBody.receiverId)
                            .get().await()
                        Log.d("me_getSentReq", "in request body")
                        val userBody = newTask.documents[0].toObject<UserBody>()
                        userBody?.let {
                            if (!reqBody.status) {
                                list.add(RequestBodyWrapper(it, reqBody))
                            }
                        }
                    }

                }
                Log.d("me_getSentReq", "success === size-->${list.size}")
                trySend(Resource.Successful(list))
            } catch (e: Exception) {
                Log.d("me_getSentReq", "Error === $e")
                trySend(Resource.Failure(e.toString()))
            }
            awaitClose()
        }
    }

    override fun getAllReceivedRequest(currentUser: FirebaseUser): Flow<Resource<List<RequestBodyWrapper>>> {
        return callbackFlow {
            try {
                val list = mutableListOf<RequestBodyWrapper>()
                val recReq =
                    fStoreReq.document(currentUser.uid).collection(RECEIVED_REQUEST).get().await()
                Log.d("me_getRecReq", "all recReq == ${recReq.documents}")
                recReq.documents.forEach { request ->
                    request.toObject<RequestBody>()?.let { reqBody ->
                        val newTask = fStoreUsers.whereEqualTo("uid", reqBody.senderId)
                            .get().await()
                        Log.d("me_getRecReq", "in request body ")
                        val userBody = newTask.documents[0].toObject<UserBody>()
                        userBody?.let {
                            if (!reqBody.status) {
                                list.add(RequestBodyWrapper(it, reqBody))
                            }
                        }
                    }

                }
                Log.d("me_getRecReq", "success === size-->${list.size}")
                trySend(Resource.Successful(list))
            } catch (e: Exception) {
                Log.d("me_getRecReq", "Error === $e")
                trySend(Resource.Failure(e.toString()))
            }

            awaitClose()
        }
    }

    private fun someMoreStuffs(chatMessage: ChatMessage, otherUser: UserBody) {
        val body = Gson().toJson(chatMessage)

        val jsonObj = JSONObject()
        val jsonNotifier = JSONObject().also {
            it.put("title", "Message")
            it.put("subtitle", "SUBTITLE")
            it.put("body", "You have a new message")
            it.put("sound", "")
        }
        jsonObj.put("to", otherUser.deviceToken.last())
        jsonObj.put("notification", jsonNotifier)
        jsonObj.put("data", JSONObject(body))


        val request = okhttp3.Request.Builder()
            .url(FCM_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader(
                "Authorization",
                WEB_KEY
            )
            .post(
                jsonObj.toString().toRequestBody(
                    "application/json; charset=utf-8".toMediaType()
                )
            ).build()


        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BASIC
        OkHttpClient.Builder().addInterceptor(logger)
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build().newCall(request)
            .enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("msg-receiver", "$e")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d(
                        "msg-receiver", "$response +++ " +
                                "${call.isExecuted()}====${response.isSuccessful}" +
                                "--${response.code}===${response.body}"
                    )
                }
            })

    }


   override fun updateToken(token: String) {
        Firebase.auth.currentUser?.let { currentUser ->
            currentUser.email?.let { email ->
                fStoreUsers.document(email).get().addOnCompleteListener { taskBody ->
                    when {
                        taskBody.isSuccessful -> {
                            taskBody.result.toObject<UserBody>()?.let { userBody ->
                                val tokenList = mutableListOf<String>()
                                tokenList.addAll(userBody.deviceToken)
                                tokenList.add(token)
                                fStoreUsers.document(email).update("deviceToken", tokenList)
                                    .addOnCompleteListener {
                                        if (it.isSuccessful) {
                                            _tokenUpdateFlow.value = RequestState.Successful(true)
                                            Log.d("me_updateToken", " ${it.result}")
                                        } else {
                                            _tokenUpdateFlow.value =
                                                RequestState.Failure(it.exception.toString())
                                            Log.d("me_updateToken", " ${it.exception}")
                                        }
                                    }
                            }
                        }
                        else -> {
                            _tokenUpdateFlow.value =
                                RequestState.Failure(taskBody.exception.toString())
                            Log.d("me_updateToken", " ${taskBody.exception}")
                        }
                    }
                }
            }
        }
    }


}

