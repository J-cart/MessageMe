package com.tutorial.messageme.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.arch.ChatsViewModel
import com.tutorial.messageme.data.models.LatestChatMessage
import com.tutorial.messageme.data.models.RecentChatAdapter
import com.tutorial.messageme.data.utils.Resource
import com.tutorial.messageme.databinding.FragmentRecentChatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecentChatsFragment : Fragment() {

    private var _binding: FragmentRecentChatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<ChatsViewModel>()

    private lateinit var adapter: RecentChatAdapter
    private val currentUser = Firebase.auth.currentUser


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRecentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUser?.let {user->
            adapter = RecentChatAdapter(user.uid)
            viewModel.addLatestMsgSnapshot(user)
            binding.recyclerView.adapter = adapter
            lifecycleScope.launch {
                viewModel.latestMsg.collect{resource->
                    when(resource){
                        is Resource.Loading->{
                            //show Loading
                            binding.progressBar.isVisible = true
                            binding.statusTv.text = "LOADING...PLEASE WAIT....."
                        }
                        is Resource.Successful->{
                            //display result
                            binding.progressBar.isVisible = false
                            resource.data?.let {list->
                                binding.statusTv.text = "Available Chats :${list.size}"
                                adapter.submitList(list)
                            }?: emptyList<LatestChatMessage>()

                            adapter.adapterClick {
                                val navigate = RecentChatsFragmentDirections.actionRecentChatsFragmentToChatsFragment(it.userBody)
                                findNavController().navigate(navigate)
                            }
                        }
                        is Resource.Failure->{
                            //show error
                            binding.progressBar.isVisible = true
                            resource.msg?.let {msg->
                                binding.statusTv.text = msg
                            }
                        }
                    }

                }
            }

        }

        binding.fabAddFriends.setOnClickListener {
            val navigate =
                RecentChatsFragmentDirections.actionRecentChatsFragmentToFriendsFragment()
            findNavController().navigate(navigate)
        }


    }

}