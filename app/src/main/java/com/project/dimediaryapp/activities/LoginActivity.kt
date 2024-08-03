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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signUpTextView = findViewById<TextView>(R.id.tvSignUp)

        signUpTextView.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            signInWithEmailAndPassword(email, password)
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String, retryCount: Int = 3) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    user?.let {
                        PreferenceHelper.saveUserId(this, it.uid) // Save user ID to shared preferences
                        retrieveDisplayNameAndSave(it)
                        checkUserProfileAndNavigate(it)
                    }
                } else {
                    if (retryCount > 0) {
                        Log.w(TAG, "signInWithEmail:retrying", task.exception)
                        signInWithEmailAndPassword(email, password, retryCount - 1)
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed. Please check your network connection and try again.",
                            Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
            }
    }

    private fun retrieveDisplayNameAndSave(user: FirebaseUser) {
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val displayName = document.getString("displayName")
                    displayName?.let {
                        PreferenceHelper.saveDisplayName(this, it)
                        updateUserProfile(user, it)
                    }
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun updateUserProfile(user: FirebaseUser, displayName: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User profile updated with display name.")
                    updateUI(user)
                }
            }
    }

    private fun checkUserProfileAndNavigate(user: FirebaseUser) {
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val cashBalance = document.getDouble("cashBalance")
                    val preferredCurrency = document.getString("preferredCurrency")

                    if (cashBalance != null && preferredCurrency != null) {
                        navigateToMainScreen()
                    } else {
                        navigateToOnboarding()
                    }
                } else {
                    navigateToOnboarding()
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
                navigateToOnboarding()
            }
    }

    private fun navigateToMainScreen() {
        val intent = Intent(this, NavigationDrawerActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Authentication success.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "User is signed out.", Toast.LENGTH_SHORT).show()
        }
    }
}
