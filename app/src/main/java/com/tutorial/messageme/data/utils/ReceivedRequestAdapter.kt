package com.tutorial.messageme.data.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tutorial.messageme.R
import com.tutorial.messageme.data.models.RequestBodyWrapper
import com.tutorial.messageme.databinding.RequestViewholderBinding

class ReceivedRequestAdapter :
    ListAdapter<RequestBodyWrapper, ReceivedRequestAdapter.ViewHolder>(DiffCallBack) {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = RequestViewholderBinding.bind(view)
        fun bind(data: RequestBodyWrapper) {
            binding.apply {
                acceptBtn.isVisible = true
                declineBtn.isVisible = true
                msg.text = data.userBody.email
                cancelBtn.setOnClickListener {
                    declineListener?.let { it1 -> it1(data) }
                }
                acceptBtn.setOnClickListener {
                    acceptListener?.let { it1 -> it1(data) }
                }
            }

        }
    }


    companion object DiffCallBack : DiffUtil.ItemCallback<RequestBodyWrapper>() {
        override fun areItemsTheSame(oldItem: RequestBodyWrapper, newItem: RequestBodyWrapper) =
            oldItem.userBody.uid == newItem.userBody.uid

        override fun areContentsTheSame(oldItem: RequestBodyWrapper, newItem: RequestBodyWrapper) =
            oldItem.userBody.uid == newItem.userBody.uid && oldItem.requestBody.msg == newItem.requestBody.msg
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReceivedRequestAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.request_viewholder, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ReceivedRequestAdapter.ViewHolder, position: Int) {
        val pos = getItem(position)
        holder.bind(pos)

    }

    private var acceptListener: ((RequestBodyWrapper) -> Unit)? = null

    fun acceptClick(listener: (RequestBodyWrapper) -> Unit) {
        acceptListener = listener
    }

    private var declineListener: ((RequestBodyWrapper) -> Unit)? = null

    fun declineClick(listener: (RequestBodyWrapper) -> Unit) {
        declineListener = listener
    }


}