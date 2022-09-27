package com.tutorial.messageme.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.arch.ChatsViewModel
import com.tutorial.messageme.data.models.RequestBody
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.RequestState
import com.tutorial.messageme.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val navArgs: ProfileFragmentArgs by navArgs()
    private val currentUser = Firebase.auth.currentUser
    private val viewModel: ChatsViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navArgs.userInfo?.let { other ->
            currentUser?.let { current ->
                binding.infoText.text = other.toString()
                observeSentReq(current, other)
//                viewModel.loadSentRequestState(current.uid, other.uid)
                viewModel.addSentRequestSnapshot(current, other)

                observeReceived(current, other)
//                viewModel.loadReceivedRequestState(current.uid, other.uid)
                viewModel.addReceivedRequestSnapshot(current, other)

                viewModel.addSpecificAcceptedSnapshot(current,other)
                observeFriendState()

            }
        } ?: "This User Doesn't Exist or Info Not Available "
        /** You could display the current user info*/
    }

    private fun observeSentReq(currentUser: FirebaseUser, otherUser: UserBody) {
        lifecycleScope.launch {
            viewModel.sentRequestStatus.collect { resource ->
                when (resource) {
                    is RequestState.Loading -> {
                        //show Loading
                        showLoading(true)
                        showSend(false)
                        showCancel(false)
                        showError(false)
                        showHandler(false)
                        binding.statusText.text = "<<<Loading>>>"

                    }
                    is RequestState.Successful -> {
                        showLoading(false)
                        showError(false)
                        showSend(false)
                        binding.statusText.text = resource.data.toString()
                        if (resource.data){
                            showCancel(false)
                        }else{
                            showCancel(true)
                            binding.cancelBtn.setOnClickListener {
                                viewModel.cancelSentRequest(currentUser, otherUser)
                            }
                        }

                    }
                    is RequestState.Failure -> {

                        showLoading(false)
                        showSend(true)
                        showHandler(false)
                        showCancel(false)
                        showError(true, resource.msg)
                    }
                    is RequestState.NonExistent -> {
                        // handle it--
                        // 1.show send btn and send request
                        //2. check request state
                        showLoading(false)
                        showSend(true)
                        showHandler(false)
                        showCancel(false)
                        showError(true, "Request doesn't exist")
                        binding.statusText.text = "Request Doesn't Exist"
                        binding.sendBtn.setOnClickListener {
                            val reqBody = RequestBody(
                                "${currentUser.email} requesting friendship from ${otherUser.email}",
                                currentUser.uid,
                                otherUser.uid,
                                false
                            )
                            viewModel.sendFriendRequest(currentUser, otherUser, reqBody)
                        }
                    }
                }

            }

        }

    }


    private fun observeReceived(currentUser: FirebaseUser, otherUser: UserBody) {
        lifecycleScope.launch {
            viewModel.receivedRequestStatus.collect { resource ->
                when (resource) {
                    is RequestState.Loading -> {
                        binding.receivedText.text = "<<<Loading>>>"
                        showHandler(false)
                    }
                    is RequestState.Successful -> {
                        binding.receivedText.text = resource.data.toString()
                        binding.requestPrompt.isVisible = !resource.data
                        if (resource.data) {
                            showHandler(false)
                        } else {
                            showHandler(true)
                            binding.acceptBtn.setOnClickListener {
                                viewModel.handleReceivedRequest(currentUser, otherUser, true)
                            }
                            binding.declineBtn.setOnClickListener {
                                viewModel.handleReceivedRequest(currentUser, otherUser, false)
                            }
                        }

                    }
                    is RequestState.Failure -> {
                        binding.receivedText.text = resource.msg
                        showHandler(false)
                    }
                    is RequestState.NonExistent -> {
                        binding.requestPrompt.isVisible = false
                        binding.receivedText.text = "Request Doesn't Exist"
                        showHandler(false)
                    }
                }
            }
        }
    }

    private fun observeFriendState(){
        lifecycleScope.launch{
            viewModel.friendsOrNot.collect{state->
                if (state){
                    //show friends icon, hide controllers
                    //don't //observeSent or //observeReceived
                    showSend(false)
                    binding.textButton.text = "FRIENDS"
                }else{
                    //show not friends
                    // observeSent and //observeReceived
                    binding.textButton.text = "NOT FRIENDS"
                }
            }
        }
    }


    private fun showError(state: Boolean, text: String = "") {
        binding.errorText.isVisible = state
        binding.errorText.text = text
    }

    private fun showSend(state: Boolean) {
        binding.sendBtn.isVisible = state
    }

    private fun showCancel(state: Boolean) {
        binding.cancelBtn.isVisible = state
    }

    private fun showLoading(state: Boolean) {
        binding.progressBar.isVisible = state
    }

    private fun showHandler(state: Boolean) {
        binding.acceptBtn.isVisible = state
        binding.declineBtn.isVisible = state
    }


}