package com.tutorial.messageme.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.arch.ChatsViewModel
import com.tutorial.messageme.data.models.LatestChatMessage
import com.tutorial.messageme.data.utils.ReceivedRequestAdapter
import com.tutorial.messageme.data.utils.Resource
import com.tutorial.messageme.databinding.FragmentReceivedRequestsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceivedRequestsFragment : Fragment() {
    private var _binding: FragmentReceivedRequestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<ChatsViewModel>()
    private val adapter by lazy { ReceivedRequestAdapter() }
    private val fAuth = Firebase.auth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentReceivedRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fAuth.currentUser?.let { user->
            viewModel.loadAllReceivedReq(user)
            observe(user)
        }
//
    }

   private fun observe(user: FirebaseUser) {
        lifecycleScope.launch {
            viewModel.allRecReqFlow.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        //show Loading
                        binding.progressBar.isVisible = true
                        binding.statusTv.text = "LOADING...PLEASE WAIT....."
                    }
                    is Resource.Successful -> {
                        //display result
                        binding.progressBar.isVisible = false
                        resource.data?.let { list ->
                            binding.statusTv.text = "Available Requests :${list.size}"
                            adapter.submitList(list)
                        } ?: emptyList<LatestChatMessage>()

                        adapter.declineClick {declineBody->
                            viewModel.handleReceivedRequest(user,declineBody.userBody,false)
                        }
                        adapter.acceptClick {acceptBody->
                            viewModel.handleReceivedRequest(user,acceptBody.userBody,true)
                        }
                    }
                    is Resource.Failure -> {
                        //show error
                        binding.progressBar.isVisible = true
                        resource.msg?.let { msg ->
                            binding.statusTv.text = msg
                        }
                    }
                }

            }
        }
    }

}