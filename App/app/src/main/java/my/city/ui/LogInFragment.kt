/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui

import android.animation.Animator
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import my.city.R
import my.city.database.FilterPattern
import my.city.database.Tags
import my.city.databinding.FragmentLogInBinding
import my.city.logic.viewmodels.State
import my.city.logic.viewmodels.UserVM
import my.city.util.PatternInputFilter

/**
 * This class sets the logic for the [LogIn][R.id.fragment_log_in] form where a user can log in with
 * its already created account in the platform
 *
 * @author Pelayo Reguera García
 * */
class LogInFragment : Fragment(R.layout.fragment_log_in) {

    private lateinit var binding: FragmentLogInBinding
    private val userVm: UserVM by activityViewModels()
    private var signInState: State = State.IN_PROCESS

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentLogInBinding.bind(view)
        binding.txteEmail.filters = arrayOf(PatternInputFilter(FilterPattern.EMAIL_CHARS))
        binding.txteEmail.setOnClickListener { binding.txtEmail.isErrorEnabled = false }
        binding.txtePassword.filters = arrayOf(PatternInputFilter(FilterPattern.PASSWORD_CHARS))
        binding.txtePassword.setOnClickListener { binding.txtPassword.isErrorEnabled = false }
        binding.btnLogIn.setOnClickListener {
            val emptyError = getString(R.string.txtEmptyError)
            var isValid = if (binding.txteEmail.text.isNullOrBlank()) {
                binding.txtEmail.error = emptyError
                false
            } else {
                binding.txtEmail.error = null
                true
            }
            isValid = if (binding.txtePassword.text.isNullOrBlank()) {
                binding.txtPassword.error = emptyError
                false
            } else {
                binding.txtPassword.error = null
                isValid
            }

            if (isValid) {
                context?.let {
                    val builder = Dialog(it)
                    val inflater = requireActivity().layoutInflater

                    val binding = inflater.inflate(R.layout.cv_dialog_animation, null)
                    val animationView = binding.findViewById<LottieAnimationView>(R.id.animIcon)

                    animationView.setAnimation(R.raw.orange_city)
                    animationView.addAnimatorListener(object : Animator.AnimatorListener {
                        private var previousState: State = State.IN_PROCESS

                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            if (signInState == State.FINISHED) {
                                builder.dismiss()
                                if (previousState == State.SUCCESS) {
                                    findNavController().navigate(
                                        SignInFragmentDirections.toFragmentMap()
                                    )
                                }
                            }
                        }

                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {
                            when (signInState) {
                                State.SUCCESS -> {
                                    // When doing this attachment, onAnimationEnd is executed, that's why
                                    // isFinished variable is needed
                                    animationView.setAnimation(R.raw.orange_tick)
                                    animationView.playAnimation()
                                    animationView.repeatCount = 0
                                    previousState = signInState
                                    signInState = State.FINISHED
                                }

                                State.IN_PROCESS, State.FINISHED -> {}
                                State.FAILURE -> {
                                    animationView.setAnimation(R.raw.orange_cross)
                                    animationView.playAnimation()
                                    animationView.repeatCount = 0
                                    signInState = State.FINISHED
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

                userVm.logIn(
                    binding.txteEmail.text.toString(),
                    binding.txtePassword.text.toString(),
                    {
                        signInState = State.SUCCESS
                    },
                    {
                        when (it) {
                            Tags.LOGIN_FAILURE -> {
                                signInState = State.FAILURE
                                Toast.makeText(
                                    context,
                                    getString(R.string.logIn_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            Tags.PROFILE_DOCUMENT_ERROR -> {
                                signInState = State.FAILURE
                                Toast.makeText(
                                    context,
                                    getString(R.string.logIn_profileDoc_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            Tags.PROFILE_PHOTO_FAILURE -> {
                                // It's not a several problem so the app continues and shows a message
                                Toast.makeText(
                                    context,
                                    getString(R.string.logIn_profilePhoto_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            else -> {// LOGIN_ERROR
                                signInState = State.FAILURE
                                Toast.makeText(
                                    context,
                                    getString(R.string.server_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                )
            }
        }
        binding.btnSignIn.setOnClickListener {
            findNavController().navigate(R.id.fragment_sign_in)
        }
    }
}