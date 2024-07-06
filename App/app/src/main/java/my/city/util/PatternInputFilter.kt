/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.util

import android.text.InputFilter
import android.text.Spanned
import my.city.database.FilterPattern

class PatternInputFilter(private val pattern: FilterPattern) : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int,
    ): CharSequence? {
        // Define a regex pattern to match only alphanumeric characters
        val pattern = Regex(pattern.value)

        // Build the resulting text after applying the input
        val resultingText = dest.toString().substring(0, dstart) +
                source.subSequence(start, end) +
                dest.toString().substring(dend, dest.length)


        // Check if the input matches the pattern
        return if (/*source.subSequence(start, end)*/resultingText.matches(pattern)) {
            null // Accept the input if it matches
        } else {
            "" // Reject the input if it does not match
        }
    }
}
