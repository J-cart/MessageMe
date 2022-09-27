package com.tutorial.messageme.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
    private val adapter by lazy {
        RecentChatAdapter()
    }


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
        binding.fabAddFriends.setOnClickListener {
            val navigate =
                RecentChatsFragmentDirections.actionRecentChatsFragmentToFriendsFragment()
            findNavController().navigate(navigate)
        }
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.latestMsg.collect{resource->
                when(resource){
                    is Resource.Loading->{
                        //show Loading
                        binding.statusTv.text = "LOADING...PLEASE WAIT....."
                    }
                    is Resource.Successful->{
                        //display result
                        resource.data?.let {
                            adapter.submitList(it)
                        }?: emptyList<LatestChatMessage>()

                        adapter.adapterClick {
                            val navigate = AllUsersFragmentDirections.actionGlobalProfileFragment(it.userBody)
                            findNavController().navigate(navigate)
                        }
                    }
                    is Resource.Failure->{
                        //show error
                        resource.msg?.let {
                            binding.statusTv.text = it
                        }
                    }
                }

            }
        }

    }

}