package com.example.contentprovider

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.RawContacts
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contentprovider.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactsAdapter: ContactsAdapter


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

        contactsAdapter = ContactsAdapter(actions)
        binding.apply {
            setSupportActionBar(toolbar)
            listRV.layoutManager = LinearLayoutManager(this@MainActivity)
            listRV.adapter = contactsAdapter
            addBTN.setOnClickListener {
                if (nameET.text.isBlank() || phoneET.text.isBlank()) {
                    Toast.makeText(this@MainActivity, "Fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                pickedContact = Contact(nameET.text.toString(), phoneET.text.toString())
                if (isPermissionGranted(Manifest.permission.WRITE_CONTACTS)) {
                    saveContact()
                } else {
                    actionOnPermissionGranted = this@MainActivity::saveContact
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isPermissionGranted(Manifest.permission.READ_CONTACTS)) {
            loadContacts()
        } else {
            actionOnPermissionGranted = this::loadContacts
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

    }

    private fun saveContact() {
        val operations = ArrayList<ContentProviderOperation>()
        operations.add(
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, pickedContact.name)
                .build()
        )
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, pickedContact.phone)
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                .build()
        )
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error adding contact", Toast.LENGTH_SHORT).show()
            }
        }.invokeOnCompletion { loadContacts() }

    }

    private fun loadContacts(){
        lifecycleScope.launch {
            binding.apply {
                progressCPI.visibility = View.VISIBLE
            }
            val contacts = withContext(Dispatchers.IO) {
                val cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )
                val result = cursor?.let {
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
                    } finally {
                        cursor.close()
                    }
                } ?: emptyList()
                result
            }
            contactsAdapter.submitList(contacts)
            binding.apply {
                listRV.visibility = View.VISIBLE
                progressCPI.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_exit -> {
                moveTaskToBack(true)
                finish()
                return true
            }
            R.id.menu_search -> {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}


