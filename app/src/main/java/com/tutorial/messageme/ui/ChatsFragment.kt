package com.tutorial.messageme.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.ktx.*
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.databinding.FragmentChatsBinding


class ChatsFragment : Fragment() {

    private val fStoreRef = Firebase.firestore.collection("Messages")
    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }



    fun performSend(){

        val mMsg = fStoreRef.document("current-user").collection("recipient-user")
            .document(System.currentTimeMillis().toString())
        mMsg.set(ChatMessage())
        val mMsg2 = fStoreRef.document("recipient-user").collection("current-user")
            .document(System.currentTimeMillis().toString())

        mMsg.set(ChatMessage())
        mMsg2.set(ChatMessage())
    }


}