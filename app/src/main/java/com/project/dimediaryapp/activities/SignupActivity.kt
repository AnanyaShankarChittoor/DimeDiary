package com.project.dimediaryapp.activities

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.project.dimediaryapp.R
import com.project.dimediaryapp.util.PreferenceHelper

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val displayNameEditText = findViewById<EditText>(R.id.etDisplayName)
        val signupButton = findViewById<Button>(R.id.btSignup)
        val signInTextView = findViewById<TextView>(R.id.tvSignIn)

        signInTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val displayName = displayNameEditText.text.toString()
            createUserWithEmailAndPassword(email, password, displayName)
        }
    }

    private fun createUserWithEmailAndPassword(email: String, password: String, displayName: String, retryCount: Int = 3) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    user?.let {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()
                        it.updateProfile(profileUpdates)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Log.d(TAG, "User profile updated.")
                                    val userDetails = hashMapOf(
                                        "displayName" to displayName,
                                        "email" to it.email  // This is now a nullable String
                                    )
                                    saveUserToFirestore(it.uid, userDetails)
                                    PreferenceHelper.saveUserId(this, it.uid)  // Save user ID to shared preferences
                                    updateUI(user)
                                }
                            }
                    }
                } else {
                    if (retryCount > 0) {
                        Log.w(TAG, "createUserWithEmail:retrying", task.exception)
                        createUserWithEmailAndPassword(email, password, displayName, retryCount - 1)
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed. Please check your network connection and try again.",
                            Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
            }
    }

    private fun saveUserToFirestore(userId: String, userDetails: HashMap<String, String?>) {
        db.collection("users").document(userId)
            .set(userDetails)
            .addOnSuccessListener { Log.d(TAG, "User added to Firestore") }
            .addOnFailureListener { e -> Log.w(TAG, "Error adding user to Firestore", e) }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Signup failed.", Toast.LENGTH_SHORT).show()
        }
    }
}
