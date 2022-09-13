package com.tutorial.messageme.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tutorial.messageme.databinding.FragmentSentRequestsBinding

class SentRequestsFragment : Fragment() {

    private var _binding: FragmentSentRequestsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

}