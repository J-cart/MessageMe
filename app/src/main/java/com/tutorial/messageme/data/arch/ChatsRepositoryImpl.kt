package com.tutorial.messageme.data.arch

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.RequestBody
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.RequestState
import com.tutorial.messageme.data.utils.Resource
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
        currentUser: String,
        otherUser: UserBody,
        requestBody: RequestBody
    ): Flow<RequestState> {
        return callbackFlow {
            val senderId = currentUser + otherUser.uid
            val receiverId = otherUser.uid + currentUser


            checkFriendRequest(currentUser, otherUser.uid).collect { exists ->
                when {
                    exists -> {
                        trySend(RequestState.Failure("Request Exists Already"))
                    }
                    else -> {
                        fStoreReq.document(currentUser).collection("requests")
                            .document(senderId)
                            .set(requestBody).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    fStoreReq.document(otherUser.uid).collection("requests")
                                        .document(receiverId)
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

    override fun checkFriendRequest(currentUserUid: String, otherUserUid: String): Flow<Boolean> =
        callbackFlow {
            fStoreReq.document(currentUserUid).collection("requests")
                .whereEqualTo("senderId", currentUserUid)
                .whereEqualTo("receiverId", otherUserUid)
                .get().addOnCompleteListener {
                    if (!it.result.isEmpty || it.result != null) {
                        trySend(true)
                        return@addOnCompleteListener
                    }
                    trySend(false)
                }
            awaitClose()
        }

    override fun getChatMessages(
        currentUser: String,
        otherUser: UserBody
    ): Flow<Resource<List<ChatMessage>>> = callbackFlow {
        fStoreMsg.document(currentUser).collection(otherUser.uid).get()
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
        currentUser: String,
        otherUser: UserBody,
        message: ChatMessage
    ): Flow<RequestState> {
        return callbackFlow {
            fStoreMsg.document(currentUser).collection(otherUser.uid)
                .document(System.currentTimeMillis().toString()).set(message)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        fStoreMsg.document(otherUser.uid).collection(currentUser)
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
}