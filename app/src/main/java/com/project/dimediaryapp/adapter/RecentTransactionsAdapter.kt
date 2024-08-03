package com.project.dimediaryapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.dimediaryapp.R
import com.project.dimediaryapp.model.Expense
import java.text.SimpleDateFormat
import java.util.Locale

class RecentTransactionsAdapter(private var expenses: List<Expense>) :
    RecyclerView.Adapter<RecentTransactionsAdapter.RecentTransactionsViewHolder>() {

    class RecentTransactionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTextView: TextView = itemView.findViewById(R.id.category_text_view)
        val amountTextView: TextView = itemView.findViewById(R.id.amount_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentTransactionsViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_transaction, parent, false)
        return RecentTransactionsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecentTransactionsViewHolder, position: Int) {
        val expense = expenses[position]
        holder.categoryTextView.text = expense.category

        // Ensure the amount is of a numeric type
        holder.amountTextView.text = String.format(Locale.getDefault(), "$%.2f", expense.amount.toDouble())

        // Format the date for display
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.dateTextView.text = expense.date?.let { dateFormat.format(it) } ?: "N/A"
    }

    override fun getItemCount(): Int {
        return expenses.size
    }

    fun updateExpenses(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
