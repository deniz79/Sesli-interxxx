package com.intercomapp.ui.contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.intercomapp.R

class FriendsAdapter(
    private val onFriendClick: (ContactsViewModel.Friend) -> Unit
) : ListAdapter<ContactsViewModel.Friend, FriendsAdapter.FriendViewHolder>(FriendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view, onFriendClick)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FriendViewHolder(
        itemView: View,
        private val onFriendClick: (ContactsViewModel.Friend) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val ivCall: ImageView = itemView.findViewById(R.id.iv_call)
        
        fun bind(friend: ContactsViewModel.Friend) {
            tvName.text = friend.name
            tvStatus.text = if (friend.isOnline) "🟢 Çevrimiçi" else "🔴 Çevrimdışı"
            
            // Set avatar (you can add custom avatars later)
            ivAvatar.setImageResource(R.drawable.ic_person)
            
            // Set call button click listener
            ivCall.setOnClickListener {
                onFriendClick(friend)
            }
            
            // Set item click listener
            itemView.setOnClickListener {
                onFriendClick(friend)
            }
        }
    }

    private class FriendDiffCallback : DiffUtil.ItemCallback<ContactsViewModel.Friend>() {
        override fun areItemsTheSame(oldItem: ContactsViewModel.Friend, newItem: ContactsViewModel.Friend): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ContactsViewModel.Friend, newItem: ContactsViewModel.Friend): Boolean {
            return oldItem == newItem
        }
    }
}
