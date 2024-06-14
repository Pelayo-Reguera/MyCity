/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import my.city.R
import my.city.databinding.FragmentEventFormBinding
import my.city.logic.viewmodels.EventCreationVM
import my.city.ui.explorer.event.challenges.EventChallengesAdapter
import my.city.util.DialogListeners

/**
 * This class sets the logic for the [Event Form][R.id.fragment_event_form] where the user can
 * create its own event with its own challenges.
 * */
class EventFormFragment : Fragment(R.layout.fragment_event_form), DialogListeners {

    /** [EventCreationVM] instance for storing the information of the event being created*/
    private val eventVM: EventCreationVM by navGraphViewModels(R.id.event_form_navigation)
    private lateinit var binding: FragmentEventFormBinding
    private var isEdited: Boolean = false
    private var challengePos: Int = 0

    /** It determines whether the device is a smartphone or other one bigger*/
    private val isLargeLayout: Boolean by lazy { resources.getBoolean(R.bool.large_layout) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventFormBinding.bind(view)
        binding.layoutRvChallenges.rvChallenges.setHasFixedSize(true)

        binding.layoutRvChallenges.rvChallenges.adapter =
            EventChallengesAdapter(
                eventVM.challenges,
                true,
                onClickEdit = { position: Int ->
                    isEdited = true
                    challengePos = position
                    showChallengeForm(position)
                },
                onClickRemove = { position: Int -> onClickRemove(position) }
            )

        binding.btniAddChallenge.setOnClickListener { showChallengeForm() }

        binding.efabReady.setOnClickListener {
            val emptyError = getString(R.string.txtEmptyError)
            var isValid = if (binding.txteEventName.text.isNullOrBlank()) {
                binding.txtEventName.error = emptyError
                false
            } else {
                binding.txtEventName.error = null
                true
            }

            isValid = if (binding.txteEventDescription.text.isNullOrBlank()) {
                binding.txtEventDescription.error = emptyError
                false
            } else {
                binding.txtEventDescription.error = null
                isValid
            }

            isValid = if (binding.txteEventLocation.text.isNullOrBlank()) {
                binding.txtEventLocation.error = emptyError
                false
            } else {
                binding.txtEventLocation.error = null
                isValid
            }

            isValid = if (binding.txteEventStartDate.text.isNullOrBlank()) {
                binding.txtEventStartDate.error = emptyError
                false
            } else {
                binding.txtEventStartDate.error = null
                isValid
            }

            isValid = if (binding.txteEventEndDate.text.isNullOrBlank()) {
                binding.txtEventEndDate.error = emptyError
                false
            } else {
                binding.txtEventEndDate.error = null
                isValid
            }

            if (isValid) {//TODO: Store the created event in database
                findNavController().navigate(
                    EventFormFragmentDirections.toFragmentExplorer()
                )
            }
        }
    }

    override fun onClickAccept(dialog: DialogFragment) {
        if (isEdited) {
            binding.layoutRvChallenges.rvChallenges.adapter?.notifyItemChanged(challengePos)
        } else {
            binding.layoutRvChallenges.rvChallenges.adapter?.notifyItemInserted(eventVM.challenges.size - 1)
        }
    }

    /**
     * Actions executed when the 'Remove' button of a challenge is pressed
     *
     * @param position The position of the challenge to be removed
     * */
    private fun onClickRemove(position: Int) {
        eventVM.challenges.removeAt(position)
        binding.layoutRvChallenges.rvChallenges.adapter?.notifyItemRemoved(position)
    }

    /**
     * Navigate to the [DialogFragment] of [ChallengeFormFragment][my.city.ui.explorer.event.challenges.ChallengeFormFragment]
     * displaying a Fragment or a Dialog depending on the size of the screen
     *
     * @param challengePos The position of the existing challenge in the list of the RecyclerView.
     * Its default value is -1 in case of creating a new challenge
     * */
    private fun showChallengeForm(challengePos: Int = -1) {
        if (isLargeLayout) {
            // The device is using a large layout, so show the fragment as a
            // dialog.
            findNavController().navigate(
                EventFormFragmentDirections.toChallengeFormFragmentDialog(
                    challengePos
                )
            )
        } else {
            // The device is smaller, so show the fragment fullscreen.
            findNavController().navigate(
                EventFormFragmentDirections.toChallengeFormFragment(
                    challengePos
                )
            )
        }
    }
}