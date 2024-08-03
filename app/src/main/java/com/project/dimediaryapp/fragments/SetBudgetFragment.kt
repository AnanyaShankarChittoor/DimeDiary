package com.project.dimediaryapp.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.slider.Slider
import com.google.firebase.firestore.FirebaseFirestore
import com.project.dimediaryapp.R
import com.project.dimediaryapp.adapter.ImageCarouselAdapter
import com.project.dimediaryapp.util.PreferenceHelper

class SetBudgetFragment : Fragment() {

    private lateinit var etSpecificGoal: EditText
    private lateinit var btnSetBudget: Button
    private lateinit var viewPager: ViewPager2
    private lateinit var slider: Slider
    private lateinit var radioGroupGoals: RadioGroup
    private lateinit var switchAlerts: Switch
    private lateinit var tvBudgetAmount: TextView
    private val handler = Handler()
    private var cashBalance: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSpecificGoal = view.findViewById(R.id.etSpecificGoal)
        btnSetBudget = view.findViewById(R.id.btn_set_budget)
        viewPager = view.findViewById(R.id.viewPager)
        slider = view.findViewById(R.id.slider)
        radioGroupGoals = view.findViewById(R.id.radioGroupGoals)
        switchAlerts = view.findViewById(R.id.switch1)
        tvBudgetAmount = view.findViewById(R.id.tv_budget_amount)

        val images = listOf(R.drawable.budget_image1, R.drawable.budget_image2, R.drawable.budget_image3)
        val adapter = ImageCarouselAdapter(images)
        viewPager.adapter = adapter

        autoScrollViewPager()

        radioGroupGoals.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.goal_save_rButton -> etSpecificGoal.visibility = View.VISIBLE
                else -> etSpecificGoal.visibility = View.GONE
            }
        }

        slider.addOnChangeListener { _, value, _ ->
            tvBudgetAmount.text = value.toInt().toString()
        }

        fetchCashBalance()

        btnSetBudget.setOnClickListener {
            val budget = tvBudgetAmount.text.toString()
            val specificGoal = etSpecificGoal.text.toString()
            val receiveAlerts = switchAlerts.isChecked
            val goalType = when (radioGroupGoals.checkedRadioButtonId) {
                R.id.manage_expense_rButton -> "Manage monthly expenses"
                R.id.goal_save_rButton -> "Save for a specific goal"
                R.id.track_spending_rButton -> "Track spending habits"
                else -> "Unknown"
            }

            if (budget.isNotEmpty()) {
                saveBudgetToFirestore(budget, goalType, specificGoal, receiveAlerts)
            } else {
                Toast.makeText(requireContext(), "Please enter a budget amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBudgetToFirestore(budget: String, goalType: String, specificGoal: String, receiveAlerts: Boolean) {
        val userId = PreferenceHelper.getUserId(requireContext())
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userBudget = hashMapOf(
                "budget" to budget,
                "goalType" to goalType,
                "specificGoal" to if (goalType == "Save for a specific goal") specificGoal else "",
                "receiveAlerts" to receiveAlerts
            )

            db.collection("users").document(userId)
                .set(userBudget)
                .addOnSuccessListener {
                    PreferenceHelper.setBudget(requireContext(), budget.toDouble())
                    Toast.makeText(requireContext(), "Budget Set: $budget", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack() // Close the fragment
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error saving budget: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "User ID not found in preferences", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchCashBalance() {
        val db = FirebaseFirestore.getInstance()
        val userId = PreferenceHelper.getUserId(requireContext()) // Retrieve the user ID from shared preferences
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        cashBalance = document.getDouble("cashBalance")?.toFloat() ?: 0f
                        slider.valueTo = cashBalance
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error fetching cash balance: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "User ID not found in preferences", Toast.LENGTH_SHORT).show()
        }
    }

    private fun autoScrollViewPager() {
        val runnable = object : Runnable {
            override fun run() {
                val currentItem = viewPager.currentItem
                val nextItem = if (currentItem == viewPager.adapter?.itemCount?.minus(1)) 0 else currentItem + 1
                viewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 3000) // Auto-scroll every 3 seconds
            }
        }
        handler.post(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // Stop auto-scrolling when the fragment is destroyed
    }
}
