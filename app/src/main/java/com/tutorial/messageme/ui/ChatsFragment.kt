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
import com.google.firebase.messaging.FirebaseMessaging
import com.tutorial.messageme.data.arch.ChatsViewModel
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.PushNotifierBody
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.*
import com.tutorial.messageme.databinding.FragmentChatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


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

        addStuffs(msg)
//        viewModel.sendMsg(currentUser, otherUser, msg)
//        binding.msgBox.text.clear()

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
                            Log.d("FCM test", "${req.raw()}, ${req.message()}, ${req.body()}")
                        } else {
                            Log.d("FCM test", " error ${req.errorBody()},body: ${req.body()},message : ${req.message()}")
                        }
                    } catch (e: Exception) {
                        Log.d("FCM test", "error:: $e, ${e.localizedMessage}, ${e.stackTrace}")
                    }
                }
            }
        }


/*
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
*/


    }


}