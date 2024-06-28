/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event.challenges

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import my.city.R
import my.city.databinding.FragmentEventChallengesBinding
import my.city.logic.viewmodels.EventVM

/**
 * This class represents the view [R.layout.fragment_event_challenges]
 * showing a list of challenges with which the user can interact
 * */
class EventChallengesFragment : Fragment(R.layout.fragment_event_challenges) {

    private lateinit var binding: FragmentEventChallengesBinding
    private val eventVM: EventVM by navGraphViewModels(R.id.event_navigation)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventChallengesBinding.bind(view)
        binding.layoutRvChallenges.rvChallenges.setHasFixedSize(true)
        eventVM.event.observe(viewLifecycleOwner) {
            binding.layoutRvChallenges.rvChallenges.adapter =
                EventChallengesAdapter(it.challenges, false)
        }
    }
}