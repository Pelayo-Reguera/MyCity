/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import my.city.R
import my.city.databinding.FragmentEventInfoBinding
import my.city.logic.viewmodels.EventVM
import java.time.format.DateTimeFormatter

class EventInfoFragment : Fragment(R.layout.fragment_event_info) {

    private lateinit var binding: FragmentEventInfoBinding
    private val eventVM: EventVM by navGraphViewModels(R.id.event_navigation)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventInfoBinding.bind(view)
        eventVM.event.observe(viewLifecycleOwner) {
            binding.txtInfoTitle.text = it.name

            val value: Int =
                it.getStartLocalDateTime().toLocalDate()
                    .compareTo(it.getEndLocalDateTime().toLocalDate())
            when {
                value < 0 -> {
                    binding.txtInfoStartDateAndHour.text = it.getStartLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                    binding.txtInfoEndDateAndHour.text = it.getEndLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                }

                value == 0 -> {
                    binding.txtInfoStartDateAndHour.text = "${
                        it.getStartLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm - "))
                    } ${
                        it.getEndLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    }"
                    binding.txtInfoEndDateAndHour.isVisible = false
                }
            }

            binding.txtInfoOrganizers
            binding.txtInfoLocation.text = it.street
            binding.txtInfoDescription.text = it.description
        }
    }
}