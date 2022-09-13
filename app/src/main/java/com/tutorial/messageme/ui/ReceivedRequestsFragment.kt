package com.tutorial.messageme.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tutorial.messageme.databinding.FragmentReceivedRequestsBinding


class ReceivedRequestsFragment : Fragment() {
    private var _binding: FragmentReceivedRequestsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentReceivedRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

}