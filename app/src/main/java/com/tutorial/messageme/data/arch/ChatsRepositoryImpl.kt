package com.tutorial.messageme.data.arch

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.RequestBody
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
            fStoreMsg.document(currentUserUid).collection(otherUserUid)
                .document(System.currentTimeMillis().toString()).set(message)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        fStoreMsg.document(otherUserUid).collection(currentUserUid)
                            .document(System.currentTimeMillis().toString()).set(message)
                        Log.d("me_message", "msg sent ")
                        trySend(RequestState.Successful(it.isComplete))
                        return@addOnCompleteListener
                    }
                    trySend(RequestState.Failure(it.exception.toString()))
                    Log.d("me_message", "msg NOT sent -- ${it.exception} ")
                }
            awaitClose()
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    override fun checkSentRequest(currentUserUid: String, otherUserUid: String): Flow<Boolean> =
        callbackFlow {
            fStoreReq.document(currentUserUid).collection(SENT_REQUEST)
                .whereEqualTo("senderId", currentUserUid)
                .whereEqualTo("receiverId", otherUserUid)
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
                            .whereEqualTo("senderId", currentUserUid)
                            .whereEqualTo("receiverId", otherUserUid)
                            .get().addOnCompleteListener { req ->

                                if (req.isComplete) {
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

    override fun addSentSnapshot(currentUser: FirebaseUser, otherUser: UserBody) {
        fStoreReq.document(currentUser.uid).collection(SENT_REQUEST)
            .whereEqualTo("senderId", currentUser.uid)
            .whereEqualTo("receiverId", otherUser.uid).addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_sentRequestSnapshot", "Listen failed. $error")
                    return@addSnapshotListener
                }
                Log.d("me_sentRequestSnapshot", "Listen failed. $error")
                getSentRequestState(currentUser.uid, otherUser.uid)

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
                            .whereEqualTo("senderId", otherUserUid)
                            .whereEqualTo("receiverId", currentUserUid)
                            .get().addOnCompleteListener { req ->

                                if (req.isComplete) {
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
                .whereEqualTo("senderId", otherUserUid)
                .whereEqualTo("receiverId", currentUserUid)
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

    override fun addReceivedSnapshot(
        currentUser: FirebaseUser, otherUser: UserBody
    ) {
        fStoreReq.document(currentUser.uid).collection(RECEIVED_REQUEST)
            .whereEqualTo("senderId", otherUser.uid)
            .whereEqualTo("receiverId", currentUser.uid).addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("me_rec_RequestSnapshot", "Listen failed. $error")
                    return@addSnapshotListener
                }
                Log.d("me_rec_RequestSnapshot", "Listen failed. $error")
                getReceivedRequestState(currentUser.uid, otherUser.uid)

            }


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
                            }?: "dummy@gmail.com"


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

   override fun getAllFriends(

    ): Flow<Resource<List<UserBody>>> {
        return callbackFlow {
            fAuth.currentUser?.let { currentUser->
                fStoreReq.document(currentUser.uid).collection(ACCEPTED_REQUEST).get()
                    .addOnCompleteListener { task ->
                        if (!task.result.isEmpty) {
                            val friendsList = mutableListOf<UserBody>()
                            task.result.documents.forEach { user ->
                                val friend = user.toObject<UserBody>()
                                friend?.let {
                                    friendsList.add(it)
                                }
                            }
                            trySend(Resource.Successful(friendsList))
                            return@addOnCompleteListener
                        }
                        trySend(Resource.Failure(task.exception.toString()))

                    }

            }?: trySend(Resource.Failure("No User is Signed In"))
            awaitClose()
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

}