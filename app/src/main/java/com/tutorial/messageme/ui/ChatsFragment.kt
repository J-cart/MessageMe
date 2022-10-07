package com.tutorial.messageme.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.tutorial.messageme.data.arch.ChatsViewModel
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.PushNotifierBody
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.*
import com.tutorial.messageme.databinding.FragmentChatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException


@AndroidEntryPoint
class ChatsFragment : Fragment() {
    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    private val chatsArgs: ChatsFragmentArgs by navArgs()
    private lateinit var chatsAdapter: ChatsAdapter
    private val fAuth = Firebase.auth
    private val viewModel by activityViewModels<ChatsViewModel>()
    private lateinit var deviceToken:List<String>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatsArgs.otherUser?.let { other ->
            fAuth.currentUser?.let { current ->
                deviceToken =  other.deviceToken
                chatsAdapter = ChatsAdapter(current.uid)
                viewModel.addMessagesSnapshot(current, other)
                getMsg()
                binding.sendBtn.setOnClickListener {
                    performSend(current, other)
                }
                binding.sendBtn2.setOnClickListener {
                    performSend2(current, other)
                }
            }
        }

    }


    private fun performSend(currentUser: FirebaseUser, otherUser: UserBody) {
        if (binding.msgBox.text.toString().trim().isEmpty()) {
            return
        }
        val msg = ChatMessage(
            senderId = currentUser.uid,
            receiverId = otherUser.uid,
            message = binding.msgBox.text.toString().trim(),
            messageType = TYPE_TEXT,
            timeStamp = System.currentTimeMillis().toString()
        )

        //viewModel.sendMsg(currentUser, otherUser, msg)
        addStuffs(msg)
        binding.msgBox.text.clear()

    }

    private fun performSend2(currentUser: FirebaseUser, otherUser: UserBody) {
        if (binding.msgBox.text.toString().trim().isEmpty()) {
            return
        }
        val msg = ChatMessage(
            senderId = currentUser.uid,
            receiverId = otherUser.uid,
            message = binding.msgBox.text.toString().trim(),
            messageType = TYPE_TEXT,
            timeStamp = System.currentTimeMillis().toString()
        )

        //viewModel.sendMsg(currentUser, otherUser, msg)
        someMoreStuffs(msg)
        binding.msgBox.text.clear()

    }

    private fun getMsg() {
        binding.chatRv.adapter = chatsAdapter
        lifecycleScope.launch {
            viewModel.userMessages.collect { resource ->
                when (resource) {
                    is Resource.Successful -> {
                        //show msg
                        resource.data?.let {
                            chatsAdapter.submitList(it)
                            binding.chatRv.scrollToPosition(chatsAdapter.itemCount - 1)
                        }
                    }
                    is Resource.Failure -> {
                        // show error
                    }
                    else -> Unit
                }
            }
        }

    }

    private fun addStuffs(chatMessage: ChatMessage) {
/*
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val header = HashMap<String, String>()
                header["Content-Type"] = "application/json"
                header["Authorization"] = WEB_KEY

                lifecycleScope.launch {

                    val pushMsg = PushNotifierBody(task.result, chatMessage)
                    try {
                        val req = ApiService.retrofitApiService.sendMsgPush(header, pushMsg)
                        if (req.isSuccessful) {
                            Log.d("FCM test", "${req.body()}")
                        } else {
                            Log.d("FCM test", " error ${req.errorBody()}")
                        }
                    } catch (e: Exception) {
                        Log.d("FCM test", "error $e")
                    }

                }


            }
        }
*/

        chatsArgs.otherUser?.let { otherUser->
                val header = HashMap<String, String>()
                header["Content-Type"] = "application/json"
                header["Authorization"] = WEB_KEY

                lifecycleScope.launch {

                    val pushMsg = PushNotifierBody(otherUser.deviceToken.last(), chatMessage)
                    try {
                        val req = ApiService.retrofitApiService.sendMsgPush(header, pushMsg)
                        if (req.isSuccessful) {
                            Log.d("FCM test", "${req.body()}")
                        } else {
                            Log.d("FCM test", " error ${req.errorBody()}")
                        }
                    } catch (e: Exception) {
                        Log.d("FCM test", "error $e")
                    }

                }
        }


    }

    private fun someMoreStuffs(chatMessage: ChatMessage) {
       /* FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val body = Gson().toJson(chatMessage)

                val jsonObj = JSONObject()
                val jsonNotifier = JSONObject().also {
                    it.put("title", "TITLE")
                    it.put("subtitle", "SUBTITLE")
                    it.put("body", "BODY")
                    it.put("sound", "Whatever Sound Available")
                }
                jsonObj.put("to", task.result)
//                jsonObj.put("notification", jsonNotifier)
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
//                    .connectTimeout(120, TimeUnit.SECONDS)
//                    .readTimeout(120, TimeUnit.SECONDS)
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
                return@addOnCompleteListener
            }
            Log.d("msg-receiver", "$ --->outside if block ${task.exception}")

        }*/

        val body = Gson().toJson(chatMessage)

        val jsonObj = JSONObject()
        val jsonNotifier = JSONObject().also {
            it.put("title", "TITLE")
            it.put("subtitle", "SUBTITLE")
            it.put("body", "BODY")
            it.put("sound", "Whatever Sound Available")
        }
        jsonObj.put("to", deviceToken?.last())
//                jsonObj.put("notification", jsonNotifier)
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
//                    .connectTimeout(120, TimeUnit.SECONDS)
//                    .readTimeout(120, TimeUnit.SECONDS)
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

}