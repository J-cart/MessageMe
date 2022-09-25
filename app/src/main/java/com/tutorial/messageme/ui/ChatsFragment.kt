package com.tutorial.messageme.ui

import android.os.Bundle
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
import com.tutorial.messageme.data.arch.ChatsViewModel
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.ChatsAdapter
import com.tutorial.messageme.data.utils.Resource
import com.tutorial.messageme.data.utils.TYPE_TEXT
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

        viewModel.sendMsg(currentUser,otherUser,msg)
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


}