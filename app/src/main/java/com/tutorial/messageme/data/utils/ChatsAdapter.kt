package com.tutorial.messageme.data.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tutorial.messageme.R
import com.tutorial.messageme.data.models.ChatMessage
import com.tutorial.messageme.databinding.ReceiverViewholderBinding
import com.tutorial.messageme.databinding.SenderViewholderBinding

class ChatsAdapter(private val uid:String) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallBack) {

    inner class FromViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = SenderViewholderBinding.bind(view)
        fun bind(data: ChatMessage) {
            binding.msg.text = data.toString()
        }
    }

    inner class ToViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ReceiverViewholderBinding.bind(view)
        fun bind(data: ChatMessage) {
            binding.msg.text = data.toString()
        }
    }


    companion object DiffCallBack : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.timeStamp == newItem.timeStamp


        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.message == newItem.message


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == SENDER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.sender_viewholder, parent, false)
            FromViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.receiver_viewholder, parent, false)
            ToViewHolder(view)
        }


        //RECEIVER


    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val pos = getItem(position)

     if (holder is FromViewHolder) {
            holder.bind(pos)
        } else {
            val viewHolder = holder as ToViewHolder
            viewHolder.bind(pos)
        }


    }

    override fun getItemViewType(position: Int): Int {
        val pos = getItem(position)

        return if (pos.senderId == uid) {
            SENDER
        } else {
            RECEIVER
        }
    }
}