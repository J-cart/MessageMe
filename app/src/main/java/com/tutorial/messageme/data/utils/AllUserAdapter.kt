package com.tutorial.messageme.data.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tutorial.messageme.R
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.databinding.UserViewholderBinding

class AllUserAdapter : ListAdapter<UserBody, AllUserAdapter.ViewHolder>(DiffCallBack) {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = UserViewholderBinding.bind(view)
        fun bind(data: UserBody) {
            binding.msg.text = data.toString()
            binding.profileBtn.setOnClickListener {
                listener?.let { it1 -> it1(data) }
            }
        }
    }


    companion object DiffCallBack : DiffUtil.ItemCallback<UserBody>() {
        override fun areItemsTheSame(oldItem: UserBody, newItem: UserBody) =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: UserBody, newItem: UserBody) =
            oldItem.email == newItem.email && oldItem.uid == newItem.uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllUserAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_viewholder, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: AllUserAdapter.ViewHolder, position: Int) {
        val pos = getItem(position)
        holder.bind(pos)

    }

    private var listener:((UserBody)->Unit)? = null

    fun adapterClick(listener:(UserBody)->Unit){
        this.listener = listener
    }


}