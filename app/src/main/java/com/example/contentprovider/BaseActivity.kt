package com.example.contentprovider

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

open class BaseActivity: AppCompatActivity() {
    protected lateinit var actionOnPermissionGranted: () -> Unit
    protected lateinit var pickedContact: Contact
    protected val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                actionOnPermissionGranted()
            } else {
                Toast.makeText(this, "Permission was denied", Toast.LENGTH_SHORT).show()
            }
        }

    protected val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGrantedMap: Map<String, Boolean> ->
            val isGranted = isGrantedMap.values.fold(true) { acc, res -> acc && res }
            if (isGranted) {
                actionOnPermissionGranted()
            } else {
                Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
            }
        }
    protected val actions = object : ContactsAdapter.ContactActions {
        override fun call(contact: Contact) {
            pickedContact = contact
            if (isPermissionGranted(Manifest.permission.CALL_PHONE)) {
                makePhoneCall()
            } else {
                actionOnPermissionGranted = this@BaseActivity::makePhoneCall
                requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }

        }

        override fun sms(contact: Contact) {
            pickedContact = contact
            if (isPermissionGranted(Manifest.permission.READ_PHONE_STATE) &&
                isPermissionGranted(Manifest.permission.SEND_SMS)) {
                sendMessage()
            } else {
                actionOnPermissionGranted = this@BaseActivity::sendMessage
                requestMultiplePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.SEND_SMS
                    )
                )
            }
        }
    }
    protected fun makePhoneCall() {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:${pickedContact.phone}")
        startActivity(intent)
    }

    protected fun sendMessage() {
        val intent = Intent(this, MessageActivity::class.java).apply {
            putExtra(MessageActivity.KEY_PHONE, pickedContact.phone)
        }
        startActivity(intent)
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}