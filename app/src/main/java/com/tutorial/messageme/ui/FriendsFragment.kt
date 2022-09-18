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
import com.tutorial.messageme.data.utils.AllFriendsAdapter
import com.tutorial.messageme.data.utils.Resource
import com.tutorial.messageme.databinding.FragmentFriendsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendsFragment : Fragment() {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<ChatsViewModel>()
    private val adapter: AllFriendsAdapter by lazy { AllFriendsAdapter() }
    private val fAuth = Firebase.auth


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
        binding.recyclerView.adapter = adapter
        fAuth.currentUser?.let {
            viewModel.loadAllFriends()
            viewModel.addAcceptedSnapshot(it)
            observeFriendsState()
        }


        binding.addUsersTV.setOnClickListener {
            val navigate = FriendsFragmentDirections.actionFriendsFragmentToAllUsersFragment()
            findNavController().navigate(navigate)
        }
        binding.statusTv.setOnClickListener {
            val navigate = FriendsFragmentDirections.actionFriendsFragmentToRequestsFragment()
            findNavController().navigate(navigate)
        }
        binding.testTv.setOnClickListener {
            val navigate = FriendsFragmentDirections.actionFriendsFragmentToChatsFragment(null)
            findNavController().navigate(navigate)
        }

    }


    private fun observeFriendsState() {
        lifecycleScope.launch {
            viewModel.allFriendsState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        //show loading
                        showLoading(true)
                        showError(false)
                    }
                    is Resource.Failure -> {
                        //show error
                        showLoading(false)
                        showError(true, "..You Currently Have No Friends..")
                        adapter.submitList(emptyList())

                    }
                    is Resource.Successful -> {
                        //show friends
                        showLoading(false)
                        showError(false)
                        resource.data?.let { list ->
                            if (list.isEmpty()) {
                                //show error
                                showError(true, "..You Currently Have No Friends..")
                                adapter.submitList(emptyList())
                            } else {
                                adapter.submitList(list)
                                adapter.adapterClick {
                                    val navigate =
                                        FriendsFragmentDirections.actionFriendsFragmentToChatsFragment(
                                            it
                                        )
                                    findNavController().navigate(navigate)
                                }

                            }
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

    private fun showLoading(state: Boolean) {
        binding.progressBar.isVisible = state
    }


}