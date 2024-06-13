/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event.challenges

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.android.material.textfield.TextInputEditText
import my.city.R
import my.city.databinding.FragmentChallengeFormBinding
import my.city.logic.Challenge
import my.city.logic.viewmodels.EventCreationVM
import my.city.util.DialogListeners

/**
 * Configures the behaviour of the view displaying the form to create or edit an existing [Challenge]
 * of a [Event][my.city.logic.Event]
 * */
class ChallengeFormFragment : DialogFragment(R.layout.fragment_challenge_form) {

    private val eventVM: EventCreationVM by navGraphViewModels(R.id.event_form_navigation)
    private lateinit var binding: FragmentChallengeFormBinding
    private var dialogListeners: DialogListeners? = null
    private val args: ChallengeFormFragmentArgs by navArgs()
    private val challengeName: TextInputEditText by lazy { binding.contentChallengeForm.txteChallengeName }
    private val challengeReward: TextInputEditText by lazy { binding.contentChallengeForm.txteChallengeReward }
    private val challengeDescription: TextInputEditText by lazy { binding.contentChallengeForm.txteChallengeDescription }

    // The system calls this to get the DialogFragment's layout, regardless of
    // whether it's being displayed as a dialog or an embedded fragment.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentChallengeFormBinding.bind(view)

        // If the is another value than the default one, means a challenge is being edited
        if (args.challengePos > -1) {
            challengeName.setText(eventVM.challenges[args.challengePos].name)
            challengeReward.setText(eventVM.challenges[args.challengePos].reward.toString())
            challengeDescription.setText(eventVM.challenges[args.challengePos].description)
        }

        // Actions for the button 'Accept' in case the dialog is shown
        binding.btnDialogAccept?.setOnClickListener {
            onComplete()
            dialogListeners?.let {
                it.onClickAccept(this)
                dialog?.dismiss()
            }
        }

        // Actions for the button 'Cancel' in case the dialog is shown
        binding.btnDialogCancel?.setOnClickListener { dialog?.dismiss() }

        // Actions for the extended button in case the extended fragment is shown
        binding.efabChallengeReady?.setOnClickListener {
            onComplete()
            findNavController().navigate(ChallengeFormFragmentDirections.toFragmentEventForm())
        }
    }

    // This function is where the main look of the dialog is configured
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // If we use context here we are accessing the activity context. As we want to access the
        // fragment we need to access the parent fragment that in the case of this app, we are
        // hosting different fragments in another which is the NavHostFragment. So once we have accessed
        // the FragmentContainerView, we need to access its child which is the NavHostFragment and then
        // the current fragment from the navigation graph used in it that is being shown in the screen
        dialogListeners =
            parentFragment?.childFragmentManager?.primaryNavigationFragment as? DialogListeners
    }

    /**
     * Actions to do when the form is completed. If it is a preselected challenge, its information
     * is changed. If it is a new one then it is added to the list of challenges
     * */
    private fun onComplete() {
        if (args.challengePos > -1) {
            eventVM.challenges[args.challengePos] = Challenge(
                challengeName.text.toString(),
                challengeDescription.text.toString(),
                "coin", //TODO: Change
                challengeReward.text.toString().toInt(),
            )
        } else {
            eventVM.challenges.add(
                Challenge(
                    challengeName.text.toString(),
                    challengeDescription.text.toString(),
                    "coin", //TODO: Change
                    challengeReward.text.toString().toInt(),
                )
            )
        }
    }
}