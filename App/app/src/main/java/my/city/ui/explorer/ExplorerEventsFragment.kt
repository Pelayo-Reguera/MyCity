/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer

import android.animation.Animator
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import my.city.R
import my.city.databinding.FragmentEventsExplorerBinding
import my.city.logic.viewmodels.EventsListVM
import my.city.logic.viewmodels.State
import my.city.logic.viewmodels.UserVM

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
    private val eventsListVM: EventsListVM by activityViewModels()
    private val userVM: UserVM by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventsExplorerBinding.bind(view)
        rvEvents = binding.rvEvents
        rvEvents.setHasFixedSize(true)

        eventsListVM.events.observe(viewLifecycleOwner) {
            rvEvents.adapter = ExplorerAdapter(it, userVM.userName.value.toString())
        }

        // Observer executed when an Event is created to show dialog
        eventsListVM.message.observe(viewLifecycleOwner) {
            context?.let {
                if (eventsListVM.processState != State.FINISHED) {
                    val builder = Dialog(it)
                    val inflater = requireActivity().layoutInflater

                    val binding = inflater.inflate(R.layout.cv_dialog_animation, null)
                    val animationView = binding.findViewById<LottieAnimationView>(R.id.animIcon)

                    animationView.setAnimation(R.raw.orange_city)
                    animationView.addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            if (eventsListVM.processState == State.FINISHED) {
                                builder.dismiss()
                            }
                        }

                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {
                            when (eventsListVM.processState) {
                                State.SUCCESS -> {
                                    // When doing this attachment, onAnimationEnd is executed, that's why
                                    // isFinished variable is needed
                                    animationView.setAnimation(R.raw.orange_tick)
                                    animationView.playAnimation()
                                    animationView.repeatCount = 0
                                    eventsListVM.processState = State.FINISHED
                                }

                                State.IN_PROCESS, State.FINISHED -> {}
                                State.FAILURE -> {
                                    animationView.setAnimation(R.raw.orange_cross)
                                    animationView.playAnimation()
                                    animationView.repeatCount = 0
                                    eventsListVM.processState = State.FINISHED
                                }
                            }
                        }
                    })

                    builder.setContentView(binding)
                    builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    builder.setCancelable(false)
                    builder.create()
                    builder.show()
                }
            }
        }

        //FIXME: Uncomment for the final product
//        binding.fabCreateEvent.isEnabled = !userVM.user.isAnonymous

        binding.fabCreateEvent.setOnClickListener {
            findNavController().navigate(ExplorerEventsFragmentDirections.toEventFormNavigation())
        }
    }
}