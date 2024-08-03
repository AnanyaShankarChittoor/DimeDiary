package com.project.dimediaryapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.project.dimediaryapp.R
import com.project.dimediaryapp.adapter.ExpensesAdapter
import com.project.dimediaryapp.model.Expense
import com.project.dimediaryapp.util.PreferenceHelper

class ExpensesViewFragment : Fragment() {

    private lateinit var recyclerViewExpenses: RecyclerView
    private lateinit var expensesAdapter: ExpensesAdapter
    private lateinit var expenseList: MutableList<Expense>
    private val categoryIconMap = mapOf(
        "Transportation" to R.drawable.ic_transportation,
        "Healthcare" to R.drawable.ic_healthcare,
        "Grocery" to R.drawable.ic_grocery,
        "Eating Out" to R.drawable.ic_eating_out,
        "Miscellaneous" to R.drawable.ic_misc
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_expenses_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        expenseList = mutableListOf()

        recyclerViewExpenses = view.findViewById(R.id.recyclerViewExpenses)
        recyclerViewExpenses.layoutManager = LinearLayoutManager(requireContext())
        expensesAdapter = ExpensesAdapter(expenseList, categoryIconMap) { expense ->
            Log.d("ExpensesViewFragment", "Clicked on expense: ${expense.name}")
            showUpdateExpenseDialog(expense)
        }
        recyclerViewExpenses.adapter = expensesAdapter

        val userId = PreferenceHelper.getUserId(requireContext())
        val budget = PreferenceHelper.getBudget(requireContext())

        if (!userId.isNullOrEmpty()) {
            fetchExpenses(userId, budget)
        } else {
            Toast.makeText(context, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchExpenses(userId: String, budget: Double?) {
        Log.d("ExpensesViewFragment", "Fetching expenses for user ID: $userId")
        val db = FirebaseFirestore.getInstance()
        db.collection("expenses")
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    var totalExpenses = 0
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        Log.d("ExpensesViewFragment", "Fetch expenses successful, documents found: ${querySnapshot.size()}")
                        for (document in querySnapshot.documents) {
                            val expense = document.toObject(Expense::class.java)?.apply {
                                id = document.id
                                totalExpenses += this.amount
                            }
                            if (expense != null) {
                                Log.d("ExpensesViewFragment", "Expense fetched: $expense")
                                expenseList.add(expense)
                            } else {
                                Log.w("ExpensesViewFragment", "Expense document is null")
                            }
                        }
                        expensesAdapter.notifyDataSetChanged()
                        Log.d("ExpensesViewFragment", "Expenses list updated: ${expenseList.size} items")
                    } else {
                        Log.d("ExpensesViewFragment", "No expenses found for user ID: $userId")
                        Toast.makeText(context, "No expenses found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("ExpensesViewFragment", "Error getting documents: ", task.exception)
                    Toast.makeText(context, "Error getting documents: ${task.exception}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showUpdateExpenseDialog(expense: Expense) {
        Log.d("ExpensesViewFragment", "Showing update dialog for expense: ${expense.name}")
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_expense, null)
        val expenseName = view.findViewById<EditText>(R.id.editTextUpdateExpenseName)
        val expenseAmount = view.findViewById<EditText>(R.id.editTextUpdateExpenseAmount)
        val switchDescription = view.findViewById<Switch>(R.id.switchUpdateDescription)
        val description = view.findViewById<EditText>(R.id.editTextUpdateExpenseDescription)

        expenseName.setText(expense.name)
        expenseAmount.setText(expense.amount.toString())
        if (expense.description.isNotEmpty()) {
            switchDescription.isChecked = true
            description.visibility = View.VISIBLE
            description.setText(expense.description)
        }

        switchDescription.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                description.visibility = View.VISIBLE
            } else {
                description.visibility = View.GONE
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Update Expense")
            .setView(view)
            .setPositiveButton("Update") { _, _ ->
                val updatedName = expenseName.text.toString()
                val updatedAmount = expenseAmount.text.toString().toInt()
                val updatedDescription = if (switchDescription.isChecked) description.text.toString() else ""

                val updatedExpense = expense.copy(
                    name = updatedName,
                    amount = updatedAmount,
                    description = updatedDescription
                )

                updateExpenseInFirestore(updatedExpense)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                deleteExpenseFromFirestore(expense)
            }
            .create()
            .show()
    }

    private fun deleteExpenseFromFirestore(expense: Expense) {
        val db = FirebaseFirestore.getInstance()
        val documentId = expense.id // Ensure expense.id contains the document ID
        db.collection("expenses").document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                val index = expenseList.indexOfFirst { it.id == expense.id }
                if (index != -1) {
                    expenseList.removeAt(index)
                    expensesAdapter.notifyItemRemoved(index)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateExpenseInFirestore(expense: Expense) {
        val db = FirebaseFirestore.getInstance()
        db.collection("expenses").document(expense.id)
            .set(expense)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Expense updated successfully", Toast.LENGTH_SHORT).show()
                val index = expenseList.indexOfFirst { it.id == expense.id }
                if (index != -1) {
                    expenseList[index] = expense
                    expensesAdapter.notifyItemChanged(index)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
