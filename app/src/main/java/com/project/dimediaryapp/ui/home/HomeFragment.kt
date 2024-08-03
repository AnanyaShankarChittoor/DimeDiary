package com.project.dimediaryapp.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.dimediaryapp.R
import com.project.dimediaryapp.databinding.FragmentHomeBinding
import com.project.dimediaryapp.fragments.AddExpenseDialogFragment
import com.project.dimediaryapp.model.Expense
import com.project.dimediaryapp.util.PreferenceHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val CHANNEL_ID = "expenses_channel"
    private val NOTIFICATION_ID = 1
    private val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    private lateinit var pieChart: PieChart
    private lateinit var totalExpensesTextView: TextView
    private lateinit var remainingBudgetTextView: TextView
    private lateinit var percentageBudgetUsedTextView: TextView
    private lateinit var lastTransactionTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        pieChart = binding.pieChart
        totalExpensesTextView = binding.totalExpensesTextView
        remainingBudgetTextView = binding.remainingBudgetTextView
        percentageBudgetUsedTextView = binding.percentageBudgetUsedTextView
        lastTransactionTextView = binding.lastTxnTextView

        val fabAddExpense: FloatingActionButton = binding.addExpense
        fabAddExpense.setOnClickListener {
            val addExpenseDialog = AddExpenseDialogFragment()
            addExpenseDialog.setOnDialogCloseListener(object : AddExpenseDialogFragment.OnDialogCloseListener {
                override fun onDialogClose() {
                    val userId = PreferenceHelper.getUserId(requireContext())
                    val budget = PreferenceHelper.getBudget(requireContext()) ?: 0.0
                    if (!userId.isNullOrEmpty()) {
                        fetchExpenses(userId, budget)
                    }
                }
            })
            addExpenseDialog.show(parentFragmentManager, "AddExpenseDialog")
        }

        createNotificationChannel()
        fetchAndStoreBudget()

        // Fetch expenses and update pie chart when the view is created
        val userId = PreferenceHelper.getUserId(requireContext())
        val budget = PreferenceHelper.getBudget(requireContext()) ?: 0.0
        if (!userId.isNullOrEmpty()) {
            fetchExpenses(userId, budget)
        }

        return root
    }

    private fun fetchAndStoreBudget() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val budgetRef = db.collection("budgets").document(userId)

        budgetRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                try {
                    val budget = document.getDouble("budget")
                    if (budget != null) {
                        // Store the budget value as needed
                        PreferenceHelper.setBudget(requireContext(), budget)
                        Log.d("HomeFragment", "Budget fetched: $budget")
                    } else {
                        Log.e("HomeFragment", "Budget field is null or not a number")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error parsing budget field: ", e)
                }
            } else {
                Log.d("HomeFragment", "No budget document found")
            }
        }.addOnFailureListener { e ->
            Log.e("HomeFragment", "Error fetching budget: ", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Expenses Notification"
            val descriptionText = "Notification for exceeding 80% of budget"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            return
        }

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Budget Alert")
            .setContentText("You have exceeded 80% of your budget.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(requireContext())) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun fetchExpenses(userId: String, budget: Double) {
        Log.d("HomeFragment", "Fetching expenses for user ID: $userId")
        val db = FirebaseFirestore.getInstance()
        db.collection("expenses")
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    var totalExpenses = 0.0
                    val categoryWiseExpenses = mutableMapOf<String, Double>()
                    val recentExpenses = mutableListOf<Expense>()
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        Log.d("HomeFragment", "Fetch expenses successful, documents found: ${querySnapshot.size()}")
                        for (document in querySnapshot.documents) {
                            val expense = document.toObject(Expense::class.java)?.apply {
                                id = document.id
                                totalExpenses += this.amount
                                val category = this.category
                                categoryWiseExpenses[category] = categoryWiseExpenses.getOrDefault(category, 0.0) + this.amount
                                recentExpenses.add(this)
                            }
                            if (expense != null) {
                                Log.d("HomeFragment", "Expense fetched: $expense")
                            } else {
                                Log.w("HomeFragment", "Expense document is null")
                            }
                        }

                        // Update the pie chart with the fetched expenses
                        updatePieChart(categoryWiseExpenses)
                        // Update summary section
                        updateSummarySection(totalExpenses, budget)
                        // Update last transaction section
                        updateLastTransactionSection(recentExpenses)

                        // Check if total expenses exceed 80% of the budget
                        if (budget > 0 && totalExpenses > 0.8 * budget) {
                            showNotification()
                        }

                    } else {
                        Log.d("HomeFragment", "No expenses found for user ID: $userId")
                        Toast.makeText(context, "No expenses found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("HomeFragment", "Error getting documents: ", task.exception)
                    Toast.makeText(context, "Error getting documents: ${task.exception}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updatePieChart(categoryWiseExpenses: Map<String, Double>) {
        val entries = categoryWiseExpenses.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

        val data = PieData(dataSet)
        dataSet.setDrawValues(false) // Hide values on the pie chart
        pieChart.data = data

        // Customize the legend
        val legend = pieChart.legend
        legend.isEnabled = true
        legend.form = Legend.LegendForm.CIRCLE
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        pieChart.description.isEnabled = false

        legend.setDrawInside(false)

        pieChart.invalidate() // refresh the chart with new data
    }

    private fun updateSummarySection(totalExpenses: Double, budget: Double) {
        val totalExpensesText = createStyledText(requireContext(), "Total Expenses", String.format("%.2f", totalExpenses))
        val remainingBudgetText = createStyledText(requireContext(), "Remaining Budget", String.format("%.2f", budget - totalExpenses))
        val percentageBudgetUsedText = createStyledText(requireContext(), "Percentage of Budget Used", String.format("%.2f%%", (totalExpenses / budget) * 100))

        totalExpensesTextView.text = totalExpensesText
        remainingBudgetTextView.text = remainingBudgetText
        percentageBudgetUsedTextView.text = percentageBudgetUsedText
    }


    private fun updateLastTransactionSection(recentExpenses: List<Expense>) {
        val container = binding.cardViewLastTxns.findViewById<LinearLayout>(R.id.transactions_container)
        container.removeAllViews() // Clear previous views

        // Add the heading
        val headingTextView = TextView(requireContext()).apply {
            text = "Last Transactions"
            setTextAppearance(R.style.CustomTextAppearanceHeadline)
        }
        container.addView(headingTextView)

        if (recentExpenses.isNotEmpty()) {
            val lastFiveExpenses = recentExpenses.sortedByDescending { it.date ?: Date(0) }.take(5)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            lastFiveExpenses.forEachIndexed { index, expense ->
                val expenseLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 16, 0, 16) // Add padding to separate transactions
                }

                // Create TextViews using the createStyledText function
                val categoryTextView = TextView(requireContext()).apply {
                    text = createStyledText(requireContext(), "Category", expense.category)
                    setTextAppearance(R.style.CustomTextAppearanceBody)
                }

                val amountTextView = TextView(requireContext()).apply {
                    text = createStyledText(requireContext(), "Amount", "$${expense.amount.toDouble()}")
                    setTextAppearance(R.style.CustomTextAppearanceBody)
                }

                val dateTextView = TextView(requireContext()).apply {
                    text = createStyledText(requireContext(), "Date", dateFormat.format(expense.date ?: Date())) // Use current date if null
                    setTextAppearance(R.style.CustomTextAppearanceBody)
                }

                // Add the TextViews to the expense layout
                expenseLayout.addView(categoryTextView)
                expenseLayout.addView(amountTextView)
                expenseLayout.addView(dateTextView)

                // Add the expense layout to the container
                container.addView(expenseLayout)

                // Add a divider view to separate transactions
                if (index < lastFiveExpenses.size - 1) {
                    val dividerView = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        ).apply {
                            setMargins(0, 8, 0, 8) // Add some vertical margin around the divider
                        }
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.divider_color)) // Use a color resource for the divider
                    }
                    container.addView(dividerView)
                }
            }
        } else {
            val noTransactionTextView = TextView(requireContext()).apply {
                text = "No transactions found"
                setTextAppearance(R.style.CustomTextAppearanceBody)
            }
            container.addView(noTransactionTextView)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                showNotification()
            } else {
                Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun createStyledText(context: Context, label: String, value: String): SpannableString {
        val text = "$label: $value"
        val spannableString = SpannableString(text)

        // Apply bold style to the label
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            label.length + 1, // Include the colon
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

}
