package com.tutorial.messageme.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.data.arch.ChatsViewModel
import com.tutorial.messageme.data.utils.RequestState
import com.tutorial.messageme.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatsViewModel by activityViewModels()
    private val fUser = Firebase.auth.currentUser


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (fUser != null) {
            val navigate = LoginFragmentDirections.actionLoginFragmentToRecentChatsFragment()
            findNavController().navigate(navigate)
            return
        }
        validateUser()

    }


    private fun validateUser() {

        binding.loginLayout.apply {
            signUpTxt.setOnClickListener {
                emailEdt.setText("")
                passEdt.setText("")
                this.root.isVisible = false
                binding.signUpLayout.root.isVisible = true
            }

            loginBtn.setOnClickListener {
                lifecycleScope.launch {
                    if(emailEdt.text.isNullOrEmpty() || passEdt.text.isNullOrEmpty()){
                        emailBox.error = "Required,Empty Field*"
                        return@launch
                    }
                    viewModel.login(emailEdt.text.toString(), passEdt.text.toString())
                    viewModel.loginState.collect {
                        when (it) {
                            is RequestState.Loading -> {
                                progressBar.isVisible = true
                            }
                            is RequestState.Successful -> {
                                progressBar.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    "Login Successful--> ${it.data}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val navigate =
                                    LoginFragmentDirections.actionLoginFragmentToRecentChatsFragment()
                                findNavController().navigate(navigate)
                            }
                            is RequestState.Failure -> {
                                progressBar.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    "Error--> ${it.msg}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is RequestState.NonExistent->{
                                progressBar.isVisible = false
                            }
                        }
                    }

                }


            }

        }

        ////////////////////////SIGN UP////////////////////////////////


        binding.signUpLayout.apply {
            loginTxt.setOnClickListener {
                emailEdt.setText("")
                passEdt.setText("")
                this.root.isVisible = false
                binding.loginLayout.root.isVisible = true
            }

            signUpBtn.setOnClickListener {
                lifecycleScope.launch {
                    if(emailEdt.text.isNullOrEmpty() || passEdt.text.isNullOrEmpty()){
                        emailBox.error = "Required,Empty Field*"
                        return@launch
                    }
                    viewModel.signUp(emailEdt.text.toString(), passEdt.text.toString())
                    viewModel.signUpState.collect {
                        when (it) {
                            is RequestState.NonExistent->{
                                progressBar.isVisible = false
                            }
                            is RequestState.Loading -> {
                                progressBar.isVisible = true
                            }
                            is RequestState.Successful -> {
                                progressBar.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    "SignUp Successful--> ${it.data}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val navigate =
                                    LoginFragmentDirections.actionLoginFragmentToRecentChatsFragment()
                                findNavController().navigate(navigate)
                            }
                            is RequestState.Failure -> {
                                progressBar.isVisible = false
                                Toast.makeText(
                                    requireContext(),
                                    "Error--> ${it.msg}",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }



}