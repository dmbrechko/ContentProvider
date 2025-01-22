package com.example.contentprovider

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.contentprovider.databinding.ListItemBinding

class ContactsAdapter(private val actions: ContactActions) :
    ListAdapter<Contact, ContactsAdapter.ContactViewHolder>(Callback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding, actions)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    interface ContactActions {
        fun call(contact: Contact)
        fun sms(contact: Contact)
    }

    class ContactViewHolder(
        private val binding: ListItemBinding,
        private val actions: ContactActions
    ) : ViewHolder(binding.root) {
        private lateinit var contact: Contact

        init {
            binding.apply {
                callIV.setOnClickListener {
                    actions.call(contact)
                }
                messageIV.setOnClickListener {
                    actions.sms(contact)
                }
            }
        }

        fun bind(contact: Contact) {
            this.contact = contact
            binding.apply {
                nameTV.text = contact.name
                phoneTV.text = contact.phone
            }
        }
    }

    class Callback() : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.phone == newItem.phone
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}