package com.project.dimediaryapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.project.dimediaryapp.R
import com.project.dimediaryapp.model.Expense

class ExpensesAdapter(
    private val expenseList: List<Expense>,
    private val categoryIconMap: Map<String, Int>,
    private val itemClickListener: (Expense) -> Unit
) : RecyclerView.Adapter<ExpensesAdapter.ExpenseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenseList[position], categoryIconMap, itemClickListener)
    }

    override fun getItemCount(): Int = expenseList.size

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val expenseName: TextView = itemView.findViewById(R.id.textViewExpenseName)
        private val expenseAmount: TextView = itemView.findViewById(R.id.textViewExpenseAmount)
        private val categoryIcon: ImageView = itemView.findViewById(R.id.imageViewCategoryIcon)
        private val cardView: CardView = itemView.findViewById(R.id.cardViewExpenseItem)

        fun bind(expense: Expense, categoryIconMap: Map<String, Int>, itemClickListener: (Expense) -> Unit) {
            expenseName.text = expense.name
            expenseAmount.text = expense.amount.toString()
            categoryIcon.setImageResource(categoryIconMap[expense.category] ?: R.drawable.ic_misc)

            cardView.setOnClickListener {
                itemClickListener(expense)
            }
        }
    }
}
