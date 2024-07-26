/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import my.city.R
import my.city.databinding.FragmentProfileJoinedEventsBinding
import my.city.logic.viewmodels.UserVM
import my.city.ui.explorer.ExplorerAdapter

class ProfileJoinedEventsFragment : Fragment(R.layout.fragment_profile_joined_events) {

    private lateinit var binding: FragmentProfileJoinedEventsBinding
    private val userVM: UserVM by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileJoinedEventsBinding.bind(view)
        binding.layoutRvEvents.rvEvents.setHasFixedSize(true)
        binding.layoutRvEvents.rvEvents.adapter = ExplorerAdapter(//TODO: Change the adapter
            userVM.joinedEvents.value ?: listOf(),
            false,
            userVM.userName.value.toString()
        )
    }
}