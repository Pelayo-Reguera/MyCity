package com.example.mycity.ui.explorer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mycity.R
import com.example.mycity.logic.dataclasses.Event

class ExplorerAdapter(private val eventsList: List<Event>) :
    RecyclerView.Adapter<ExplorerAdapter.EventViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val eventTitle: TextView = view.findViewById(R.id.txtTitle)
        private val eventSubTitle: TextView = view.findViewById(R.id.txtSubTitle)
        private val eventImg: ImageView = view.findViewById(R.id.imgEvent)

        fun bindEvent(event: Event) {
            eventTitle.text = event.title
            eventSubTitle.text = event.organizer
        }
    }

    // Create new cards (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): EventViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.card_recycler_event, viewGroup, false)

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