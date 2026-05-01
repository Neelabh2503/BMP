package com.edgesite.yolov8tflite

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object ChannelAuthManager {

    private const val TAG        = "ChannelAuth"
    private const val COLLECTION = "channels"

    private val db by lazy { FirebaseFirestore.getInstance() }

    fun verify(
        channel  : String,
        password : String,
        onSuccess: ()          -> Unit,
        onFailure: (String)    -> Unit
    ) {
        if (channel.isBlank()) { onFailure("Channel ID cannot be empty."); return }
        if (password.isBlank()) { onFailure("Password cannot be empty."); return }

        db.collection(COLLECTION)
            .document(channel.trim())
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Log.w(TAG, "Channel not found: $channel")
                    onFailure("Channel \"$channel\" not found.")
                    return@addOnSuccessListener
                }

                val active = doc.getBoolean("active") ?: true
                if (!active) {
                    onFailure("Channel \"$channel\" is disabled.")
                    return@addOnSuccessListener
                }

                val storedHash = doc.getString("password_hash") ?: ""
                val inputHash  = sha256(password.trim())

                if (inputHash == storedHash) {
                    Log.i(TAG, "Auth OK for channel: $channel")
                    onSuccess()
                } else {
                    Log.w(TAG, "Wrong password for channel: $channel")
                    onFailure("Incorrect password.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Auth check failed: ${e.message}")
                onFailure("Connection error: ${e.message}")
            }
    }

    fun hashPassword(plain: String): String = sha256(plain)
    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
