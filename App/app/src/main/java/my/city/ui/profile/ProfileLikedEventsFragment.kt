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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import my.city.R
import my.city.databinding.FragmentProfileJoinedEventsBinding
import my.city.logic.Event
import my.city.logic.viewmodels.EventsListVM
import my.city.logic.viewmodels.UserVM
import my.city.ui.explorer.ExplorerAdapter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A simple [Fragment] subclass used for displaying a list with events which the user has joined
 * until the moment
 */
class ProfileLikedEventsFragment : Fragment(R.layout.fragment_profile_liked_events) {

    private lateinit var binding: FragmentProfileJoinedEventsBinding
    private val userVM: UserVM by activityViewModels()
    private val eventsListVM: EventsListVM by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileJoinedEventsBinding.bind(view)
        binding.layoutRvEvents.rvEvents.setHasFixedSize(true)
        if (userVM.isAnonymous.value == false) {
            val likedEvents: MutableList<Event> = mutableListOf()
            userVM.likedEventsIds.observe(viewLifecycleOwner) {
                // It tries to search the created event locally, otherwise it is requested to the
                // remote database
                lifecycleScope.launch {// A new coroutine because we don't want to wait for all events
                    val processes: MutableList<Job> = mutableListOf()
                    for (id in it) {
                        processes.add(launch {// It asynchronously adds each event to the list
                            suspendCoroutine {
                                try {
                                    eventsListVM.events.value?.first { event -> event.id == id }
                                            ?.let { event ->
                                                likedEvents.add(event)
                                                it.resume(Unit)
                                            }
                                } catch (_: NoSuchElementException) {
                                    eventsListVM.getEventInfo(id) { event ->
                                        likedEvents.add(event)
                                        it.resume(Unit)
                                    }
                                }
                            }
                        })
                    }

                    processes.joinAll()
                    binding.layoutRvEvents.rvEvents.adapter =
                        ExplorerAdapter(//TODO: Change the adapter
                                likedEvents,
                                false,
                                userVM.userName.value.toString()
                        )
                }
            }
        }
    }
}