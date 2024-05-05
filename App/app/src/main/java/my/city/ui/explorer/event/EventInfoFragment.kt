/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import my.city.R
import my.city.databinding.FragmentEventInfoBinding

class EventInfoFragment : Fragment(R.layout.fragment_event_info) {

    private lateinit var binding: FragmentEventInfoBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventInfoBinding.bind(view)
    }
}