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
import my.city.R
import my.city.databinding.FragmentEventChallengesBinding
import my.city.logic.Challenge

class EventChallengesFragment : Fragment(R.layout.fragment_rvevent_challenges) {

    private lateinit var binding: FragmentEventChallengesBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventChallengesBinding.bind(view)
        binding.rvChallenges.setHasFixedSize(true)

        val challengesList: MutableList<Challenge> = mutableListOf(
            Challenge("Desafio1", "Descripcion1", "coin1", 150),
            Challenge("Desafio2", "Descripcion2", "coin2", 250),
            Challenge("Desafio3", "Descripcion3", "coin3", 100),
            Challenge("Desafio4", "Descripcion4", "coin4", 400),
            Challenge("Desafio5", "Descripcion5", "coin5", 300),
        )

        binding.rvChallenges.adapter = EventChallengesAdapter(challengesList)
    }
}