package com.example.contentprovider

import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contentprovider.databinding.ActivitySearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : BaseActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var contactsAdapter: ContactsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        contactsAdapter = ContactsAdapter(actions)
        binding.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener {
                finish()
            }
            listRV.layoutManager = LinearLayoutManager(this@SearchActivity)
            listRV.adapter = contactsAdapter
            searchBTN.setOnClickListener {
                if (nameET.text.isBlank()) {
                    Toast.makeText(this@SearchActivity, "Enter something to search", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                searchContacts(nameET.text.toString())
            }
        }
    }

    private fun searchContacts(str: String){
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
            }.filter { it.name.contains(str) }
            contactsAdapter.submitList(contacts)
            binding.apply {
                listRV.visibility = View.VISIBLE
                progressCPI.visibility = View.GONE
            }
        }
    }
}