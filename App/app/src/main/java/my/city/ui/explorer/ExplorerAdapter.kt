/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import my.city.R
import my.city.logic.Event
import java.time.format.DateTimeFormatter

/**
 * This class is in charge of generating components based on a layout which later will be
 * used in a recycler view
 * */
class ExplorerAdapter(private val eventsList: List<Event>) :
    RecyclerView.Adapter<ExplorerAdapter.EventViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val imgEvent: ImageView = view.findViewById(R.id.imgEvent)
        private val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        private val txtSubTitle: TextView = view.findViewById(R.id.txtSubTitle)
        private val txtDescription: TextView = view.findViewById(R.id.txtDescription)
        private val btnAttendance: MaterialButton = view.findViewById(R.id.btnAttendance)

        fun bindEvent(event: Event) {
            txtTitle.text = event.name
            txtSubTitle.text =
                event.startEvent.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
            txtDescription.text = event.location.street
            btnAttendance.text = "Join"
        }
    }

    // Create new cards (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): EventViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.card_event, viewGroup, false)

        view.setOnClickListener { card ->
            card.findNavController().navigate(R.id.to_fragment_event)
        }

        return EventViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: EventViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bindEvent(eventsList[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = eventsList.size
}