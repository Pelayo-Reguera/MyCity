/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui

import android.animation.Animator
import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.Timestamp
import com.tomtom.sdk.common.measures.DistanceFormatter
import com.tomtom.sdk.common.measures.UnitSystem
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.search.Search
import com.tomtom.sdk.search.SearchCallback
import com.tomtom.sdk.search.SearchOptions
import com.tomtom.sdk.search.SearchResponse
import com.tomtom.sdk.search.common.error.SearchFailure
import com.tomtom.sdk.search.model.SearchResultType
import com.tomtom.sdk.search.online.OnlineSearch
import com.tomtom.sdk.search.ui.SearchResultClickListener
import com.tomtom.sdk.search.ui.model.PlaceDetails
import com.tomtom.sdk.search.ui.model.toPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.city.R
import my.city.database.FilterPattern
import my.city.database.RemoteDatabase
import my.city.database.Tags
import my.city.databinding.FragmentSignInBinding
import my.city.logic.viewmodels.State
import my.city.logic.viewmodels.UserVM
import my.city.util.PatternInputFilter
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * This class sets the logic for the [SignIn][R.id.fragment_sign_in] form where a user can creates
 * its account in the platform
 *
 * @author Pelayo Reguera García
 * */
class SignInFragment : Fragment(R.layout.fragment_sign_in) {

    private lateinit var binding: FragmentSignInBinding
    private val userVm: UserVM by activityViewModels()

    private var isNameUsed: Boolean = false
    private var profilePhoto: Uri? = null
        set(value) {
            field = value
            if (value == null && ::binding.isInitialized) {
                binding.imgUser.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.default_profile,
                        null,
                    )
                )
            }
        }
    private var location: GeoPoint? = null
    private var birthDate: LocalDate = LocalDate.now()

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            context?.let {
                val request = ImageRequest.Builder(it)
                    .data(uri)
                    .target(onSuccess = { drawable ->
                        profilePhoto = uri
                        binding.imgUser.setImageDrawable(drawable)
                        binding.btniRemoveImg.isVisible = true
                        binding.btniRemoveImg.isClickable = true
                    })
                    .build()
                it.imageLoader.enqueue(request)
            }
        }

    private val onlineSearch: Search by lazy {
        OnlineSearch.create(
            requireContext(),
            "De8ig7uYydhNPpGTnNS0PMqAQxagES3R" //TODO: Remove
        )
    }

    /** This property is used for performance purposes with the TomTom's API*/
    private var isSearching: Boolean = false

    /** Informs whether there is a new input to consume in a search*/
    private var requestSearch: Boolean = false
    private var signInState: State = State.IN_PROCESS

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSignInBinding.bind(view)

        binding.btniEditImg.setOnClickListener {
            pickMedia.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        binding.btniRemoveImg.setOnClickListener {
            profilePhoto = null
            binding.btniRemoveImg.isVisible = false
            binding.btniRemoveImg.isClickable = false
        }

        binding.txteName.filters = arrayOf(PatternInputFilter(FilterPattern.USER_NAME))
        binding.txteName.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus && !binding.txteName.text.isNullOrBlank()) {
                RemoteDatabase.getPublicUserInfo(binding.txteName.text.toString(), {
                    isNameUsed = true
                }, {
                    isNameUsed = false
                })
            }
        }
        binding.txteName.setOnClickListener { binding.txtName.isErrorEnabled = false }
        binding.txteEmail.filters = arrayOf(PatternInputFilter(FilterPattern.EMAIL_CHARS))
        binding.txteEmail.setOnClickListener { binding.txtEmail.isErrorEnabled = false }
        binding.txtePassword.filters = arrayOf(PatternInputFilter(FilterPattern.PASSWORD_CHARS))
        binding.txtePassword.setOnClickListener { binding.txtPassword.isErrorEnabled = false }
        binding.txteConfirmationPassword.filters =
            arrayOf(PatternInputFilter(FilterPattern.PASSWORD_CHARS))
        binding.txteConfirmationPassword.setOnClickListener {
            binding.txtConfirmationPassword.isErrorEnabled = false
        }

        binding.txteLocation.doOnTextChanged { text, _, _, _ ->
            location = null
            requestSearch = !text.isNullOrBlank()
            // This 'if' avoids to overload with many requests
            if (!isSearching) {
                lifecycleScope.launch(Dispatchers.Default) {
                    makeSearchRequest()
                }
            }
        }

        binding.txteLocation.setOnClickListener { binding.txtLocation.isErrorEnabled = false }
        binding.txteLocation.setOnFocusChangeListener { _, hasFocus ->
            binding.searchResultsView.isVisible = hasFocus
        }

        // When an option is clicked its text is written in the EditText
        binding.searchResultsView.searchResultClickListener = SearchResultClickListener {
            binding.txteLocation.setText(it.address)
            location = GeoPoint(it.position.latitude, it.position.longitude)
            binding.searchResultsView.isVisible = false
        }

        binding.txteUserBirthday.setOnClickListener { showDatePicker() }

        binding.btnReady.setOnClickListener {
            val emptyError = getString(R.string.txtEmptyError)
            var isValid = if (binding.txteName.text.isNullOrBlank()) {
                binding.txtName.error = emptyError
                false
            } else if (isNameUsed) {
                binding.txtName.error = getString(R.string.user_form_txteName_error)
                false
            } else {
                binding.txtName.error = null
                true
            }

            isValid = if (binding.txteEmail.text.isNullOrBlank()) {
                binding.txtEmail.error = emptyError
                false
            } else if (!binding.txteEmail.text!!.matches(Regex(FilterPattern.EMAIL.value))) {
                binding.txtEmail.error = getString(R.string.user_form_txteEmail_error)
                false
            } else {
                binding.txtEmail.error = null
                isValid
            }

            isValid = if (binding.txtePassword.text.isNullOrBlank()) {
                binding.txtPassword.error = emptyError
                false
            } else if (!binding.txtePassword.text!!.contains(Regex(".*?[a-z].*"))) {
                binding.txtPassword.error = getString(R.string.user_form_txtePsswrd_lowerCase_error)
                false
            } else if (!binding.txtePassword.text!!.contains(Regex(".*?[A-Z].*"))) {
                binding.txtPassword.error = getString(R.string.user_form_txtePsswrd_upperCase_error)
                false
            } else if (!binding.txtePassword.text!!.contains(Regex(".*?\\d.*"))) {
                binding.txtPassword.error = getString(R.string.user_form_txtePsswrd_number_error)
                false
            } else if (!binding.txtePassword.text!!.contains(Regex(".*?[@$!%*?&)(].*"))) {
                binding.txtPassword.error =
                    getString(R.string.user_form_txtePsswrd_specialChar_error) + ": @$!%*?&)("
                false
            } else if (binding.txtePassword.text!!.length < 8) {
                binding.txtPassword.error = getString(R.string.user_form_txtePsswrd_length_error)
                false
            } else {
                binding.txtPassword.error = null
                isValid
            }

            isValid =
                if (binding.txteConfirmationPassword.text.isNullOrBlank()) {
                    binding.txtConfirmationPassword.error = emptyError
                    false
                } else if (binding.txteConfirmationPassword.text.toString() != binding.txtePassword.text.toString()) {
                    binding.txtConfirmationPassword.error =
                        getString(R.string.user_form_txteConfirmPsswrd_error)
                    false
                } else {
                    binding.txtConfirmationPassword.error = null
                    isValid
                }

            isValid = if (binding.txteLocation.text.isNullOrBlank()) {
                binding.txtLocation.error = emptyError
                false
            } else if (location == null) {
                binding.txtLocation.error = getString(R.string.location_not_selected)
                false
            } else {
                binding.txtLocation.error = null
                isValid
            }

            isValid = if (binding.txteUserBirthday.text.isNullOrBlank()) {
                binding.txtUserBirthday.error = emptyError
                false
            } else if (!birthDate.isBefore(LocalDate.now().minusYears(15))) {
                binding.txtUserBirthday.error =
                    getString(R.string.user_form_txteUserBirthdate_error)
                false
            } else {
                binding.txtUserBirthday.error = null
                isValid
            }

            isValid = if (binding.txteGender.text.isNullOrBlank()) {
                binding.txtGender.error = emptyError
                false
            } else {
                binding.txtGender.error = null
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
                            previousState = signInState
                            when (signInState) {
                                State.SUCCESS -> {
                                    // When doing this attachment, onAnimationEnd is executed, that's why
                                    // isFinished variable is needed
                                    animationView.setAnimation(R.raw.orange_tick)
                                    animationView.playAnimation()
                                    animationView.repeatCount = 0
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

                userVm.signIn(
                    profilePhoto,
                    binding.txteName.text.toString(),
                    binding.txteEmail.text.toString(),
                    binding.txtePassword.text.toString(),
                    location ?: GeoPoint(39.56885126840019, -3.301451387486932),
                    Timestamp(
                        Date(
                            (birthDate.atStartOfDay(ZoneId.systemDefault())).toInstant()
                                .toEpochMilli()
                        )
                    ),
                    binding.txteGender.text.toString(),
                    {
                        signInState = State.SUCCESS
                    },
                    {
                        when (it) {
                            Tags.EXISTING_EMAIL_FAILURE -> {
                                signInState = State.FAILURE
                                Toast.makeText(
                                    context,
                                    getString(R.string.signIn_existingEmail_err),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            Tags.PROFILE_PHOTO_FAILURE -> {
                                // The app continue despite this problem so a state is not set because
                                // it depends on other factors
                                Toast.makeText(
                                    context,
                                    getString(R.string.signIn_profilePhoto_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            else -> {
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
    }

    /**
     * This function is in charge of making the requests to search for places. It controls whether
     * to accept the text and make a request or not. Also ensures that the last input provided by
     * the user while it was searching for places is consumed, otherwise the text written while is
     * searching wouldn't be searched and the results would be outdated
     * */
    private fun makeSearchRequest() {
        isSearching = true
        requestSearch = false
        val text = binding.txteLocation.text
        // The text need to be checked again because it could be modified since the call to itself
        // in the callback
        if (!text.isNullOrBlank()) {
            onlineSearch.search(
                SearchOptions(
                    text.toString(),
                    limit = 5,
                    resultTypes = setOf(SearchResultType.Area),
                ),
                object : SearchCallback {
                    override fun onFailure(failure: SearchFailure) {
                        binding.searchResultsView.displayError(getString(R.string.search_location_error))
                        if (requestSearch) {
                            makeSearchRequest()
                        }
                        isSearching = false
                    }

                    override fun onSuccess(result: SearchResponse) {
                        result.results.map {
                            context?.let { it1 ->
                                it.toPlace(
                                    UnitSystem.Metric,
                                    DistanceFormatter(it1)
                                )
                            }
                        }.let { binding.searchResultsView.update(it as List<PlaceDetails>) }
                        if (requestSearch) {
                            makeSearchRequest()
                        }
                        isSearching = false
                    }
                })
        } else {
            isSearching = false
        }
    }

    private fun showDatePicker() {
        context?.let {
            val calendar: Calendar = Calendar.getInstance()
            val dateDialog = DatePickerDialog(it)
            dateDialog.setOnDateSetListener { view, year, month, day ->
                val date = LocalDate.of(year, month, day)
                birthDate = date
                binding.txteUserBirthday.setText(
                    date.format(DateTimeFormatter.ofPattern("dd/LL/uuuu"))
                )
            }
            calendar.add(Calendar.YEAR, -15)
            dateDialog.datePicker.maxDate = calendar.timeInMillis
            dateDialog.show()
        }
    }
}
