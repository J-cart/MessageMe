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

class SentRequestAdapter :
    ListAdapter<RequestBodyWrapper, SentRequestAdapter.ViewHolder>(DiffCallBack) {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = RequestViewholderBinding.bind(view)
        fun bind(data: RequestBodyWrapper) {
            binding.apply {
                cancelBtn.isVisible = true
                msg.text = data.userBody.email
                cancelBtn.setOnClickListener {
                    listener?.let { it1 -> it1(data) }
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
    ): SentRequestAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.request_viewholder, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: SentRequestAdapter.ViewHolder, position: Int) {
        val pos = getItem(position)
        holder.bind(pos)

    }

    private var listener: ((RequestBodyWrapper) -> Unit)? = null

    fun adapterClick(listener: (RequestBodyWrapper) -> Unit) {
        this.listener = listener
    }


}