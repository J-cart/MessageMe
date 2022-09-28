package com.tutorial.messageme.data.arch

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.models.*
import com.tutorial.messageme.data.utils.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatsRepositoryImpl : ChatsRepository {

    private val fAuth = Firebase.auth
    private val fStoreUsers = Firebase.firestore.collection("Users")
    private val fStoreMsg = Firebase.firestore.collection("Messages")
    private val fStoreReq = Firebase.firestore.collection("Friend_Requests")


    override fun signUpNew(email: String, password: String): Flow<RequestState> {
        return callbackFlow {
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
                                trySend(RequestState.Successful(signUp.isComplete && addUser.isComplete))
                            } else {
                                Log.d("me_addUsers", "ERROR--->${addUser.exception}")
                                addUser.exception?.message?.let {
                                    trySend(RequestState.Failure(it))
                                } ?: RequestState.Failure("Unable to save user data...")
                            }

                        }
                    }
                    else -> {
                        Log.d("me_signUp", "ERROR--->${signUp.exception}")
                        signUp.exception?.message?.let {
                            trySend(RequestState.Failure(it))
                        } ?: RequestState.Failure("Error While Signing Up...")

                    }
                }
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
        otherUserUid: String,
        message: ChatMessage
    ): Flow<RequestState> {
        return callbackFlow {
            /*      fStoreMsg.document(currentUserUid).collection(otherUserUid)
                      .document(System.currentTimeMillis().toString()).set(message)
                      .addOnCompleteListener {
                          if (it.isSuccessful) {
                              fStoreMsg.document(otherUserUid).collection(currentUserUid)
                                  .document(System.currentTimeMillis().toString()).set(message)
                              Log.d("me_message", "msg sent ")
                              /////////////////////////////////////////////////
                              fStoreMsg.document(LATEST_MSG).collection(currentUserUid)
                                  .document(otherUserUid)
                                  .set(latestChatMessage)
                              fStoreMsg.document(LATEST_MSG).collection(otherUserUid)
                                  .document(currentUserUid)
                                  .set(latestChatMessage)
                              trySend(RequestState.Successful(it.isComplete))
                              ////////////////////////////////////////////////
                              return@addOnCompleteListener
                          }
                          trySend(RequestState.Failure(it.exception.toString()))
                          Log.d("me_message", "msg NOT sent -- ${it.exception} ")
                      }*/

            val msgRef = fStoreMsg.document(currentUserUid).collection(otherUserUid)
                .document(System.currentTimeMillis().toString())
            val lstMsgRefCur =
                fStoreMsg.document(LATEST_MSG).collection(currentUserUid).document(otherUserUid)
            val lstMsgRefOther =
                fStoreMsg.document(LATEST_MSG).collection(otherUserUid).document(currentUserUid)
            val curMsgBody = LatestMsgBody(otherUserUid, message)
            val otherMsgBody = LatestMsgBody(currentUserUid, message)
            Firebase.firestore.runBatch { batch ->
                batch.set(msgRef, message)
                batch.set(lstMsgRefCur, curMsgBody)
                batch.set(lstMsgRefOther, otherMsgBody)
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(RequestState.Successful(task.isComplete))
                    Log.d("me_message", "msg sent ")
                    return@addOnCompleteListener
                }
                Log.d("me_message", "msg NOT sent -- ${task.exception} ")
                trySend(RequestState.Failure(task.exception.toString()))
            }
            Firebase.firestore.runTransaction { transaction ->
                fStoreUsers.whereEqualTo("uid", currentUserUid)
                    .get().result.documents[0].toObject<UserBody>()
                transaction.get(fStoreUsers.document(""))
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
                            fStoreReq.document(currentUser.uid).collection(ACCEPTED_REQUEST).get()
                                .addOnCompleteListener { task ->
                                    val friendsList = mutableListOf<UserBody>()
                                    if (!task.result.isEmpty) {
                                        friendsList.clear()
                                        task.result.documents.forEach { user ->
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
                                        return@addOnCompleteListener
                                    }
                                    trySend(Resource.Failure(task.exception.toString()))
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
                val collection = fStoreMsg.document(LATEST_MSG).collection(currentUser.uid).get().await()
                collection.documents.forEach { message->
                    Log.d("me_latestMsgTest", "in collection")
                    message.toObject<LatestMsgBody>()?.let {latestMsg ->
                        val new =  fStoreUsers.whereEqualTo("uid", latestMsg.uid)
                            .get().await()
                        Log.d("me_latestMsgTest", "in newUser")
                        val userBody = new.documents[0].toObject<UserBody>()
                        userBody?.let {
                            msgList.add( LatestChatMessage(it,latestMsg.chatMessage))
                        }
                    }

                }
                Log.d("me_latestMsgTest", "success ===> $msgList")
                trySend(Resource.Successful(msgList))
            }catch (e:Exception){
                Log.d("me_latestMsgTest", "exception ---$e")
            }


/*
            fStoreMsg.document(LATEST_MSG).collection(currentUser.uid).get()
                .addOnCompleteListener { task ->
                    when {
                        task.isSuccessful -> {
                            val msgList = mutableListOf<LatestChatMessage>()
                            if (!task.result.isEmpty) {
                                msgList.clear()
                                task.result.documents.forEach { msg ->
                                    msg.toObject<LatestMsgBody>()?.let { latestMsgBody ->
                                        Log.d("me_latestMsgTest", "UID +++ ${latestMsgBody.uid}")
                                        fStoreUsers.whereEqualTo("uid", latestMsgBody.uid)
                                            .get().addOnCompleteListener { task2 ->
                                                if(task2.isSuccessful){
                                                    Log.d("me_latestMsgTest", "Success======>not empty ${task2.result.documents}")
                                                    val userBody =
                                                        task2.result.documents[0].toObject<UserBody>()
                                                    userBody?.let {
                                                        val msgBody = LatestChatMessage(
                                                            it,
                                                            latestMsgBody.chatMessage
                                                        )
                                                        msgList.add(msgBody)
                                                    }
                                                }else{
                                                    Log.d("me_latestMsgTest", "exception -->${task2.exception}")
                                                }


                                            }

                                    }
                                }
                                Log.d("me_latestMsgTest", "Sent Resource to Ui==$msgList")
                                trySend(Resource.Successful(msgList))
                                return@addOnCompleteListener
                            }
                            Log.d("me_latestMsgTest", "exception ---${task.exception}")
                            trySend(Resource.Failure(task.exception.toString()))
                        }
                        else -> {
                            Log.d("me_latestMsgTest", "exception ---${task.exception}")
                            trySend(Resource.Failure(task.exception.toString()))
                        }

                    }

                }
*/
            awaitClose()
        }

    }


    fun getLatest(currentUser: FirebaseUser): Flow<Resource<List<LatestChatMessage>>> {

        return callbackFlow {
            try {

                val msgList = mutableListOf<LatestChatMessage>()
                val collection = fStoreMsg.document(LATEST_MSG).collection(currentUser.uid).get().await()
                collection.documents.forEach { message->
                    message.toObject<LatestMsgBody>()?.let {latestMsg ->
                        val new =  fStoreUsers.whereEqualTo("uid", latestMsg.uid)
                            .get().await()
                        val userBody = new.documents[0].toObject<UserBody>()
                        userBody?.let {
                            msgList.add( LatestChatMessage(it,latestMsg.chatMessage))
                        }
                    }

                }
                trySend(Resource.Successful(msgList))
            }catch (e:Exception){
                Log.d("me_latestMsgTest", "exception ---$e")
            }
/*
            fStoreMsg.document(LATEST_MSG).collection(currentUser.uid).get()
                .addOnCompleteListener { task ->
                    when {
                        task.isSuccessful -> {
                            val msgList = mutableListOf<LatestChatMessage>()
                            if (!task.result.isEmpty) {
                                msgList.clear()
                                task.result.documents.forEach { msg ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        msg.toObject<LatestMsgBody>()?.let { latestMsgBody ->
                                            Log.d("me_latestMsgTest", "UID +++ ${latestMsgBody.uid}")
                                          try {
                                              val new =  fStoreUsers.whereEqualTo("uid", latestMsgBody.uid)
                                                  .get().await()
                                              val userBody = new.documents[0].toObject<UserBody>()
                                             userBody?.let {
                                                 msgList.add( LatestChatMessage(it,latestMsgBody.chatMessage))

                                             }
                                              Log.d("me_latestMsgTest", "latest msg Sent")
                                          }catch (e:Exception){
                                              Log.d("me_latestMsgTest", "exception ---$e")
                                          }

                                        }
                                    }
                                }
                                Log.d("me_latestMsgTest", "Sent Resource to Ui==$msgList")
                                trySend(Resource.Successful(msgList))
                                return@addOnCompleteListener
                            }
                            Log.d("me_latestMsgTest", "exception ---${task.exception}")
                            trySend(Resource.Failure(task.exception.toString()))
                        }
                        else -> {
                            Log.d("me_latestMsgTest", "exception ---${task.exception}")
                            trySend(Resource.Failure(task.exception.toString()))
                        }

                    }

                }
*/
            awaitClose()
        }

    }

}

