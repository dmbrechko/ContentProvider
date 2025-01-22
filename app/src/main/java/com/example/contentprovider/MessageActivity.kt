package com.example.contentprovider

import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.contentprovider.databinding.ActivityMainBinding
import com.example.contentprovider.databinding.ActivityMessageBinding

class MessageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val error = getString(R.string.error)
        val phone = intent.getStringExtra(KEY_PHONE) ?: error
        binding.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener {
                finish()
            }
            phoneTV.text = phone
            sendBTN.setOnClickListener {
                if (messageET.text.isBlank()) {
                    Toast.makeText(this@MessageActivity, "Enter message", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (phone == error) {
                    Toast.makeText(this@MessageActivity, "Error retrieving phone number", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                try {
                    val message = messageET.text.toString()
                    val smsManager = getSystemService(SmsManager::class.java) // min api 24 not check needed
                    smsManager.sendTextMessage(phone, null, message, null, null)
                    Toast.makeText(this@MessageActivity, "Message was sent", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MessageActivity, "Error sending message", Toast.LENGTH_SHORT).show()
                }
                messageET.text.clear()
            }
        }
    }

    companion object {
        const val KEY_PHONE = "key phone"
    }
}