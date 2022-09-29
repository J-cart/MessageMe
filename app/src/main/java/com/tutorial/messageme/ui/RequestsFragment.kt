package com.tutorial.messageme.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.tutorial.messageme.databinding.FragmentFriendRequestsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RequestsFragment : Fragment() {
    private var _binding: FragmentFriendRequestsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFriendRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = listOf(
            FragmentListWrapper(SentRequestsFragment(),"SENT"),
            FragmentListWrapper(ReceivedRequestsFragment(),"RECEIVED")
        )

        val tabMediator = TabLayoutMediator(binding.tabLayout,binding.viewPager){tab,position->
            tab.text = list[position].title
        }
        binding.viewPager.adapter = ViewPagingAdapter(this,list)
        tabMediator.attach()
    }

    class ViewPagingAdapter(fragment: Fragment, private val list:List<FragmentListWrapper>) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            return list.size
        }

        override fun createFragment(position: Int): Fragment {
            return list[position].fragment
        }
    }
    data class FragmentListWrapper(val fragment: Fragment,val title:String)
}


