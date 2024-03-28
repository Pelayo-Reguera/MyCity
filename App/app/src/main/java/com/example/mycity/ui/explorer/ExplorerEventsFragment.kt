/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package com.example.mycity.ui.explorer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycity.R
import com.example.mycity.databinding.FragmentEventsExplorerBinding
import com.example.mycity.logic.dataclasses.Event
import java.util.Date

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
//        binding = FragmentEventsRecyclerBinding.inflate(inflater)
        rvEvents = binding.rvEvents
        rvEvents.setHasFixedSize(true)

        val layoutManager = GridLayoutManager(context, 2)
        rvEvents.layoutManager = layoutManager

        val eventsList = mutableListOf<Event>(
            Event(
                "a", "Evento1", "Evento de ejemplo", mutableListOf(), "Mi casa",
                Date(), Date(), mutableListOf()
            ),
            Event(
                "a", "Evento1", "Evento de ejemplo", mutableListOf(), "Mi casa",
                Date(), Date(), mutableListOf()
            ),
            Event(
                "a", "Evento1", "Evento de ejemplo", mutableListOf(), "Mi casa",
                Date(), Date(), mutableListOf()
            )
        )
        val eventsAdapter = ExplorerAdapter(eventsList)
        rvEvents.adapter = eventsAdapter
    }
}