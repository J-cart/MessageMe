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
import com.google.firebase.firestore.auth.User
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
                observeReq(current,other)
                viewModel.getRequestState(current, other)
                viewModel.addRequestSnapshot(current,other)
            }
        } ?: "This User Doesn't Exist or Info Not Available "
        /** You could display the current user info*/
    }

    private fun observeReq(currentUser:FirebaseUser,otherUser: UserBody) {
        lifecycleScope.launch {

            viewModel.requestStatus.collect { resource ->
                when (resource) {
                    is RequestState.Loading -> {
                        //show Loading
                        showLoading(true)
                        showBtn(false)
                        showError(false)
                        binding.statusText.text = "<<<Loading>>>"

                    }
                    is RequestState.Successful -> {
                        showLoading(false)
                        showError(false)
                        showBtn(true)
                        binding.statusText.text = resource.data.toString()

                    }
                    is RequestState.Failure -> {

                        showLoading(false)
                        showBtn(true)
                        showError(true, resource.msg)
                    }
                    is RequestState.NonExistent -> {
                        // handle it--
                        // 1.show send btn and send request
                        //2. check request state
                        showLoading(false)
                        showBtn(true)
                        showError(true, "Request doesn't exist")
                        binding.sendBtn.setOnClickListener {
                            val reqBody = RequestBody(
                                "${currentUser.email} requesting friendship from ${otherUser.email}",
                                currentUser.uid,
                                otherUser.uid,
                                false
                            )
                            viewModel.sender(currentUser,otherUser,reqBody)
                            viewModel.getRequestState(currentUser, otherUser)
                        }
                    }
                }

            }

        }

    }

    private fun showError(state: Boolean, text: String = "") {
        binding.errorText.isVisible = state
        binding.errorText.text = text
    }

    private fun showBtn(state: Boolean) {
        binding.sendBtn.isVisible = state
    }

    private fun showLoading(state: Boolean) {
        binding.progressBar.isVisible = state
    }


}