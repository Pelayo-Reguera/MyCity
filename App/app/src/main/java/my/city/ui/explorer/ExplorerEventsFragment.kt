/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.GeoPoint
import my.city.R
import my.city.databinding.FragmentEventsExplorerBinding
import my.city.logic.Event
import my.city.logic.Location
import my.city.logic.User
import java.time.LocalDateTime

/**
 * [Fragment] containing a list of upcoming events cards
 *
 * @author Pelayo Reguera García
 * */
// The XMl of the layout is passed as an argument to the Fragment's constructor to create and
// attach it to the parent (the FragmentContainerView in the MainActivity) to do more things during
// this process, override the method onCreateView (which returns "return inflater.inflate(R.layout
// .<<corresponding layout>>, container, false)")
class ExplorerEventsFragment : Fragment(R.layout.fragment_events_explorer) {

    private lateinit var binding: FragmentEventsExplorerBinding
    private lateinit var rvEvents: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventsExplorerBinding.bind(view)
        rvEvents = binding.rvEvents
        rvEvents.setHasFixedSize(true)

        //TODO: Extract events from the database
        val eventsList: MutableList<Event> = mutableListOf(
            Event(
                "Los40 Music awards",
                mutableListOf(
                    User(
                        "User1",
                        "email1@gmail.com",
                        "photoURL",
                        mutableListOf(),
                        mutableMapOf()
                    )
                ),
                "Evento de ejemplo",
                mutableListOf(),
                Location(GeoPoint(0.0, 0.0), "Mi casa"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                mutableListOf(),
            ),
            Event(
                "b",
                mutableListOf(
                    User(
                        "User1",
                        "email1@gmail.com",
                        "photoURL",
                        mutableListOf(),
                        mutableMapOf()
                    )
                ),
                "Evento de ejemplo",
                mutableListOf(),
                Location(GeoPoint(0.0, 0.0), "Mi casa"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                mutableListOf(),
            ),
            Event(
                "c",
                mutableListOf(
                    User(
                        "User1",
                        "email1@gmail.com",
                        "photoURL",
                        mutableListOf(),
                        mutableMapOf()
                    )
                ),
                "Evento de ejemplo",
                mutableListOf(),
                Location(GeoPoint(0.0, 0.0), "Mi casa"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                mutableListOf(),
            )
        )
        val eventsAdapter = ExplorerAdapter(eventsList)
        rvEvents.adapter = eventsAdapter
        binding.fabCreateEvent.setOnClickListener {
            findNavController().navigate(ExplorerEventsFragmentDirections.toEventFormNavigation())
        }
    }
}