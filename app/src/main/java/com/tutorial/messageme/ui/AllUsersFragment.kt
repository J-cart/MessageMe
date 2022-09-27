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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.arch.ChatsViewModel
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.data.utils.AllUserAdapter
import com.tutorial.messageme.data.utils.Resource
import com.tutorial.messageme.databinding.FragmentAllUsersBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllUsersFragment : Fragment() {
    private var _binding: FragmentAllUsersBinding? = null
    private val binding get() = _binding!!
    private val viewModel:ChatsViewModel by activityViewModels()
    private val fStoreMsg = Firebase.firestore.collection("Messages")
    private lateinit var otherUser:UserBody
    private val adapter by lazy {
        AllUserAdapter()
    }


    //TODO :
    // 1. get all the app users
    // 2. use te recyclerview to enable sending request and other stuffs



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAllUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            observeAllUsers()
        }
    }

    private suspend fun observeAllUsers(){

        viewModel.loadAllUsers()
        viewModel.allUsers.collect{resource->
            when(resource){
                is Resource.Loading->{
                    //show Loading
                    errorState(false)
                    loadingState(true)
                }
                is Resource.Successful->{
                    //display result
                    errorState(false)
                    loadingState(false)
                    binding.recyclerView.adapter = adapter
                    resource.data?.let {
                        adapter.submitList(it)
                    }?: emptyList<UserBody>()

                    adapter.adapterClick {
                        val navigate = AllUsersFragmentDirections.actionGlobalProfileFragment(it)
                        findNavController().navigate(navigate)
                    }
                }
                is Resource.Failure->{
                    //show error
                    loadingState(false)
                    errorState(true)
                    resource.msg?.let {
                        binding.errorText.text = it
                    }
                }
            }

        }
    }

    private fun errorState(state:Boolean){
        binding.errorText.isVisible = state
    }
    private fun loadingState(state:Boolean){
        binding.progressBar.isVisible = state
    }

}