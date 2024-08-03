package com.project.dimediaryapp.util

import android.content.Context
import com.project.dimediaryapp.R

class Util {

    companion object {
        fun getCurrencyAbbreviation(context: Context, preferredCurrency: String?): String {
            val currencyNames = context.resources.getStringArray(R.array.currency_names)
            val currencyAbbr = context.resources.getStringArray(R.array.currency_abbr)

            val index = currencyNames.indexOf(preferredCurrency)
            return if (index != -1) currencyAbbr[index] else ""
        }
    }
}
