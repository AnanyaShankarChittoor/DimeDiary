package com.project.dimediaryapp.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.project.dimediaryapp.R
import com.project.dimediaryapp.util.FirestoreUtil
import com.project.dimediaryapp.util.PreferenceHelper
import java.util.Date

class AddExpenseDialogFragment : DialogFragment() {

    interface OnDialogCloseListener {
        fun onDialogClose()
    }

    private var listener: OnDialogCloseListener? = null

    private lateinit var expenseName: EditText
    private lateinit var expenseAmount: EditText
    private lateinit var switchDescription: Switch
    private lateinit var description: EditText
    private lateinit var chipGroupCategories: ChipGroup

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_add_expense_dialog, null)

        expenseName = view.findViewById(R.id.expenseName)
        expenseAmount = view.findViewById(R.id.expenseAmount)
        switchDescription = view.findViewById(R.id.switchDescription)
        description = view.findViewById(R.id.description)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)

        switchDescription.setOnCheckedChangeListener { _, isChecked ->
            description.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Expense")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                if (validateInputs()) {
                    saveExpenseToFirestore()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onDialogClose()
    }

    fun setOnDialogCloseListener(listener: OnDialogCloseListener) {
        this.listener = listener
    }

    private fun validateInputs(): Boolean {
        return when {
            expenseName.text.isNullOrEmpty() -> {
                showToast("Expense Name is required")
                false
            }
            expenseAmount.text.isNullOrEmpty() -> {
                showToast("Amount is required")
                false
            }
            chipGroupCategories.checkedChipId == View.NO_ID -> {
                showToast("Category is required")
                false
            }
            else -> true
        }
    }

    private fun getSelectedChipValue(): String? {
        val selectedChipId = chipGroupCategories.checkedChipId
        return if (selectedChipId != View.NO_ID) {
            val selectedChip: Chip? = chipGroupCategories.findViewById(selectedChipId)
            selectedChip?.text?.toString()
        } else {
            null
        }
    }

    private fun saveExpenseToFirestore() {
        val userId = PreferenceHelper.getUserId(requireContext())
        if (userId == null) {
            showToast("User ID is required")
            return
        }

        val name = expenseName.text.toString()
        val amount = expenseAmount.text.toString().toDoubleOrNull()
        val category = getSelectedChipValue()
        val descriptionText = if (description.visibility == View.VISIBLE) description.text.toString() else ""

        if (category == null || amount == null) {
            showToast("Valid amount and category are required")
            return
        }

        val expense = hashMapOf(
            "userId" to userId,
            "name" to name,
            "amount" to amount,
            "category" to category,
            "description" to descriptionText,
            "date" to Timestamp(Date())
        )

        FirestoreUtil(requireContext()).createOrUpdateDocument(
            collection = "expenses",
            documentId = null,  // Let Firestore generate a unique ID
            data = expense
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
