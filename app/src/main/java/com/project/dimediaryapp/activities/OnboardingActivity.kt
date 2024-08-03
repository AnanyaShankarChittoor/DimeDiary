package com.project.dimediaryapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.dimediaryapp.R
import com.project.dimediaryapp.databinding.ActivityCurrencySelectionBinding
import com.project.dimediaryapp.util.FirestoreUtil
import com.project.dimediaryapp.util.PreferenceHelper

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCurrencySelectionBinding
    private lateinit var firestoreUtil: FirestoreUtil
    val userDbName ="users"

    companion object {
        private const val TAG = "OnboardingActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreUtil = FirestoreUtil(this)

        setupCurrencySpinner()
        setupListeners()
    }

    private fun setupCurrencySpinner() {
        val currencyOptions = resources.getStringArray(R.array.currency_names)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spCurrency.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            val selectedCurrency = binding.spCurrency.selectedItem.toString()
            savePreferredCurrency(selectedCurrency)
            PreferenceHelper.savePreferredCurrency(this, selectedCurrency)
            val intent = Intent(this, CashBalanceActivity::class.java)
            startActivity(intent)
        }
    }

    private fun savePreferredCurrency(currency: String) {
        val userId = PreferenceHelper.getUserId(this)
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userDetails = hashMapOf(
            "preferredCurrency" to currency
        )

        Log.d(TAG, "Saving preferred currency for user $userId: $currency")
        firestoreUtil.createOrUpdateDocument("users", userId, userDetails)  // Ensure this uses userId
    }

}
