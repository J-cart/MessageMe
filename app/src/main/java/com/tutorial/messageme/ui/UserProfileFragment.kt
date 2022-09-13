package com.tutorial.messageme.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tutorial.messageme.databinding.FragmentUserProfileBinding

class UserProfileFragment : Fragment() {
    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sb = StringBuilder()
        sb.append(Firebase.auth.currentUser?.uid ?: " No uid \n")
        sb.append(Firebase.auth.currentUser?.email ?: "No email ")
        binding.settingsTv.text = sb
        binding.statusTv.setOnClickListener {
            val navigate = UserProfileFragmentDirections.actionUserProfileFragmentToEditProfileFragment()
            findNavController().navigate(navigate)
        }

        binding.settingsTv.setOnClickListener {
            Firebase.auth.signOut()
            val navigate = UserProfileFragmentDirections.actionUserProfileFragmentToLoginFragment()
            findNavController().navigate(navigate)
        }

    }

}