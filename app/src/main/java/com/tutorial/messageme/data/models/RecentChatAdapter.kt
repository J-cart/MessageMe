package com.tutorial.messageme.data.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tutorial.messageme.R
import com.tutorial.messageme.databinding.RecentChatsViewholderBinding

class RecentChatAdapter :
    ListAdapter<LatestChatMessage, RecentChatAdapter.ViewHolder>(DiffCallBack) {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = RecentChatsViewholderBinding.bind(view)
        fun bind(data: LatestChatMessage) {
            binding.msg.text = data.chatMessage.message
            binding.userBody.text= data.userBody.email
            binding.root.setOnClickListener {
                listener?.let { it1 -> it1(data) }
            }
        }
    }


    companion object DiffCallBack : DiffUtil.ItemCallback<LatestChatMessage>() {
        override fun areItemsTheSame(oldItem: LatestChatMessage, newItem: LatestChatMessage) =
            oldItem.userBody.uid == newItem.userBody.uid

        override fun areContentsTheSame(oldItem: LatestChatMessage, newItem: LatestChatMessage) =
            oldItem.userBody.uid == newItem.userBody.uid && oldItem.chatMessage.message == newItem.chatMessage.message
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecentChatAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recent_chats_viewholder, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: RecentChatAdapter.ViewHolder, position: Int) {
        val pos = getItem(position)
        holder.bind(pos)

    }

    private var listener: ((LatestChatMessage) -> Unit)? = null

    fun adapterClick(listener: (LatestChatMessage) -> Unit) {
        this.listener = listener
    }


}