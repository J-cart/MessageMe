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
import com.tutorial.messageme.data.utils.Resource
import com.tutorial.messageme.databinding.FragmentFriendsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendsFragment : Fragment() {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<ChatsViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //TODO: 1. show all friends
        binding.addUsersTV.setOnClickListener {
            val navigate = FriendsFragmentDirections.actionFriendsFragmentToAllUsersFragment()
            findNavController().navigate(navigate)
        }
        binding.statusTv.setOnClickListener {
            val navigate = FriendsFragmentDirections.actionFriendsFragmentToRequestsFragment()
            findNavController().navigate(navigate)
        }
        binding.testTv.setOnClickListener {
            val navigate = FriendsFragmentDirections.actionFriendsFragmentToChatsFragment()
            findNavController().navigate(navigate)
        }

    }


    private fun observeFriendsState() {
        lifecycleScope.launch {
            viewModel.allFriendsState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        //show loading
                    }
                    is Resource.Failure -> {
                        //show error
                    }
                    is Resource.Successful -> {
                        //show friends
                    }

                }

            }
        }

    }

}