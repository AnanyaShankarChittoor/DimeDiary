package com.project.dimediaryapp.util

import android.content.Context
import android.content.SharedPreferences

object PreferenceHelper {

    private const val PREF_NAME = "user_prefs"
    private const val USER_ID_KEY = "user_id"
    private const val PREFERRED_CURRENCY_KEY = "preferred_currency"
    private const val DISPLAY_NAME_KEY = "display_name"
    private const val BUDGET = "budget"



    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }


    fun getBudget(context: Context): Double? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(BUDGET, null)?.toDoubleOrNull()
    }

    fun setBudget(context: Context, budget: Double) {
        val editor = getPreferences(context).edit()
        editor.putString(BUDGET, budget.toString())
        editor.apply()
    }
    fun saveUserId(context: Context, userId: String) {
        val editor = getPreferences(context).edit()
        editor.putString(USER_ID_KEY, userId)
        editor.apply()
    }

    fun getUserId(context: Context): String? {
        return getPreferences(context).getString(USER_ID_KEY, null)
    }

    fun savePreferredCurrency(context: Context, currency: String) {
        val editor = getPreferences(context).edit()
        editor.putString(PREFERRED_CURRENCY_KEY, currency)
        editor.apply()
    }

    fun getPreferredCurrency(context: Context): String? {
        return getPreferences(context).getString(PREFERRED_CURRENCY_KEY, null)
    }

    fun saveDisplayName(context: Context, displayName: String) {
        val editor = getPreferences(context).edit()
        editor.putString(DISPLAY_NAME_KEY, displayName)
        editor.apply()
    }

    fun getDisplayName(context: Context): String? {
        return getPreferences(context).getString(DISPLAY_NAME_KEY, null)
    }
}
