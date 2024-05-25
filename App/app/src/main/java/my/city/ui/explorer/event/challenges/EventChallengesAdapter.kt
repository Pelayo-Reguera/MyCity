/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event.challenges

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import my.city.R
import my.city.logic.Challenge

/**
 * This class is in charge of generating components based on a layout which later will be
 * used in a recycler view
 * */
class EventChallengesAdapter(private val challengesList: List<Challenge>) :
    RecyclerView.Adapter<EventChallengesAdapter.ChallengeViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ChallengeViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val imgChallenge: ImageView = view.findViewById(R.id.imgChallenge)
        private val txtTitle: TextView = view.findViewById(R.id.txtChallengeName)
        private val txtReward: TextView = view.findViewById(R.id.txtChallengeReward)
        private val txtDescription: TextView = view.findViewById(R.id.txtChallengeDesc)

        fun bindEvent(challenge: Challenge) {
            txtTitle.text = challenge.name
            txtReward.text = challenge.reward.toString()
        }
    }

    // Create new cards (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ChallengeViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.card_challenge, viewGroup, false)

        view.setOnClickListener { //TODO: Show the detailed information about the challenge
            val txtDescription: TextView = view.findViewById(R.id.txtChallengeDesc)
            txtDescription.visibility =
                if (txtDescription.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        return ChallengeViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ChallengeViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bindEvent(challengesList[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = challengesList.size
}