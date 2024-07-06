/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.findNavController
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import com.tomtom.sdk.map.display.marker.BalloonViewAdapter
import com.tomtom.sdk.map.display.marker.Marker
import my.city.R
import my.city.logic.Event
import java.time.format.DateTimeFormatter

class CustomBalloonViewAdapter(private val context: Context, private val events: List<Event>) :
    BalloonViewAdapter {
    override fun onCreateBalloonView(marker: Marker): View {
        val view = LayoutInflater.from(context).inflate(R.layout.balloon_event, null)
        val event = events[marker.balloonText.toInt()]

        if (event.eventDrawables.size > 0) {
            view.findViewById<ShapeableImageView>(R.id.imgEvent)
                .setImageDrawable(event.eventDrawables[0])
        }
        view.findViewById<MaterialTextView>(R.id.txtEventName).text = event.name
        view.findViewById<MaterialTextView>(R.id.txtStartEventDate).text =
            event.getStartLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))

        view.setOnClickListener {
            it.findNavController().navigate(MapFragmentDirections.toFragmentEvent(event.id))
        }

        return view
    }
}