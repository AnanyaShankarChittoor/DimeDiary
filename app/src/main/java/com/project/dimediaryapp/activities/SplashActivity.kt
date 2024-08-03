package com.project.dimediaryapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.project.dimediaryapp.R


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Find the button and set a click listener
        val startButton: Button = findViewById(R.id.start_button)
        startButton.setOnClickListener {
            // Start the MainActivity
           val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Close this activity
            finish()
        }
    }
}
