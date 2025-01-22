package com.example.contentprovider

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.contentprovider.databinding.ActivityMainBinding
import com.example.contentprovider.databinding.ListItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var actionOnPermissionGranted: () -> Unit
    private lateinit var pickedContact: Contact
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                actionOnPermissionGranted()
            } else {
                Toast.makeText(this, "Permission was denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGrantedMap: Map<String, Boolean> ->
            val isGranted = isGrantedMap.values.fold(true) { acc, res -> acc && res }
            if (isGranted) {
                actionOnPermissionGranted()
            } else {
                Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val actions = object : ContactsAdapter.ContactActions {
            override fun call(contact: Contact) {
                pickedContact = contact
                if (isPermissionGranted(Manifest.permission.CALL_PHONE)) {
                    makePhoneCall()
                } else {
                    actionOnPermissionGranted = this@MainActivity::makePhoneCall
                    requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }

            }

            override fun sms(contact: Contact) {
                pickedContact = contact
                if (isPermissionGranted(Manifest.permission.READ_PHONE_STATE) &&
                    isPermissionGranted(Manifest.permission.SEND_SMS)) {
                    sendMessage()
                } else {
                    actionOnPermissionGranted = this@MainActivity::sendMessage
                    requestMultiplePermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.SEND_SMS
                        )
                    )
                }
            }
        }
        contactsAdapter = ContactsAdapter(actions)
        binding.apply {
            listRV.layoutManager = LinearLayoutManager(this@MainActivity)
            listRV.adapter = contactsAdapter
        }
    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            listRV.visibility = View.GONE
            progressCPI.visibility = View.VISIBLE
        }
        if (isPermissionGranted(Manifest.permission.READ_CONTACTS)) {
            loadContacts()
        } else {
            actionOnPermissionGranted = this::loadContacts
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

    }

    fun loadContacts(){
        lifecycleScope.launch(Dispatchers.IO) {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            val contacts = cursor?.let {
                try {
                    val list = mutableListOf<Contact>()
                    val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val phoneIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex)
                        val phone = cursor.getString(phoneIndex)
                        if (name != null && phone != null) {
                            list.add(Contact(name, phone))
                        }
                    }
                    list
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
            cursor?.close()
            withContext(Dispatchers.Main.immediate) {
                contactsAdapter.submitList(contacts)
                binding.apply {
                    listRV.visibility = View.VISIBLE
                    progressCPI.visibility = View.GONE
                }
            }
        }
    }

    fun makePhoneCall() {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:${pickedContact.phone}")
        startActivity(intent)
    }

    fun sendMessage() {
        val intent = Intent(this, MessageActivity::class.java).apply {
            putExtra(MessageActivity.KEY_PHONE, pickedContact.phone)
        }
        startActivity(intent)
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}

data class Contact(val name: String, val phone: String)
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