package com.project.dimediaryapp.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.dimediaryapp.R
import com.project.dimediaryapp.databinding.ActivityCashBalanceBinding
import com.project.dimediaryapp.util.FirestoreUtil
import com.project.dimediaryapp.util.PreferenceHelper
import com.project.dimediaryapp.util.Util
class CashBalanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCashBalanceBinding
    private lateinit var firestoreUtil: FirestoreUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCashBalanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreUtil = FirestoreUtil(this)

        val preferredCurrency = PreferenceHelper.getPreferredCurrency(this)
        binding.tvCurrency.text = Util.getCurrencyAbbreviation(this,preferredCurrency).toString()

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            val cashAmount = binding.etCashAmount.text.toString().toDoubleOrNull()
            if (cashAmount != null) {
                saveCashBalance(cashAmount)
                val intent = Intent(this, NavigationDrawerActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Invalid cash amount", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etCashAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Update any UI if needed when text changes
            }
        })
    }

    private fun saveCashBalance(cashBalance: Double) {
        val userId = PreferenceHelper.getUserId(this)
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userBalance = hashMapOf(
            "cashBalance" to cashBalance
        )

        firestoreUtil.createOrUpdateDocument("users", userId, userBalance,
            callback = object : FirestoreUtil.FirestoreCallback {
                override fun onComplete(success: Boolean, documentId: String?) {
                    if (success) {
                        Toast.makeText(this@CashBalanceActivity, "Cash balance saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CashBalanceActivity, "Error saving cash balance", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

}
