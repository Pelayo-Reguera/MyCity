/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.firebase.firestore.GeoPoint
import com.tomtom.sdk.common.measures.DistanceFormatter
import com.tomtom.sdk.common.measures.UnitSystem
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.location.android.AndroidLocationProvider
import com.tomtom.sdk.search.Search
import com.tomtom.sdk.search.SearchCallback
import com.tomtom.sdk.search.SearchOptions
import com.tomtom.sdk.search.SearchResponse
import com.tomtom.sdk.search.common.error.SearchFailure
import com.tomtom.sdk.search.online.OnlineSearch
import com.tomtom.sdk.search.ui.SearchResultClickListener
import com.tomtom.sdk.search.ui.model.PlaceDetails
import com.tomtom.sdk.search.ui.model.toPlace
import my.city.R
import my.city.databinding.FragmentEventFormBinding
import my.city.logic.Event
import my.city.logic.Location
import my.city.logic.viewmodels.EventCreationVM
import my.city.logic.viewmodels.EventsListVM
import my.city.logic.viewmodels.State
import my.city.ui.explorer.event.challenges.EventChallengesAdapter
import my.city.util.DialogListeners

/**
 * This class sets the logic for the [Event Form][R.id.fragment_event_form] where the user can
 * create its own event with its own challenges.
 * */
class EventFormFragment : Fragment(R.layout.fragment_event_form), DialogListeners {

    private lateinit var androidLocationProvider: LocationProvider

    /** [EventCreationVM] instance for storing the information of the event being created*/
    private val eventVM: EventCreationVM by navGraphViewModels(R.id.event_form_navigation)
    private val globalEventsVM: EventsListVM by activityViewModels()
    private lateinit var binding: FragmentEventFormBinding
    private var isEdited: Boolean = false
    private var challengePos: Int = 0
    private val onlineSearch: Search by lazy {
        OnlineSearch.create(
            requireContext(),
            "De8ig7uYydhNPpGTnNS0PMqAQxagES3R"
        )
    }
    private var locationChosen: PlaceDetails? = null

    /** This property is used for performance puposes with the TomTom's API*/
    private var isSearching = false

    /** It determines whether the device is a smartphone or other bigger*/
    private val isLargeLayout: Boolean by lazy { resources.getBoolean(R.bool.large_layout) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventFormBinding.bind(view)

        binding.txteEventLocation.doOnTextChanged { text, start, before, count ->
            locationChosen = null
            if (!text.isNullOrBlank() && !isSearching) {
                isSearching = true
                onlineSearch.search(
                    SearchOptions(
                        text.toString(),
                        limit = 5,
                        geoBias = androidLocationProvider.lastKnownLocation?.position
                    ),
                    object : SearchCallback {
                        override fun onFailure(failure: SearchFailure) {
                            binding.searchResultsView.displayError(getString(R.string.search_location_error))
                            isSearching = false
                        }

                        override fun onSuccess(result: SearchResponse) {
                            isSearching = false
                            result.results.map {
                                context?.let { it1 ->
                                    it.toPlace(
                                        UnitSystem.Metric,
                                        DistanceFormatter(it1)
                                    )
                                }
                            }.let { binding.searchResultsView.update(it as List<PlaceDetails>) }
                        }
                    })
            }
        }

        binding.txteEventLocation.setOnFocusChangeListener { v, hasFocus ->
            binding.searchResultsView.isVisible = hasFocus
        }

        // When an option is clicked its text is written in the EditText
        binding.searchResultsView.searchResultClickListener = SearchResultClickListener {
            binding.txteEventLocation.setText(it.name)
            locationChosen = it
        }

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

            isValid = if (locationChosen == null) {
                binding.txtEventLocation.error = getString(R.string.location_not_selected)
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

            if (isValid) {
                saveTmpEventChanges()
                globalEventsVM.addEvent(
                    Event(
                        eventVM.name,
                        eventVM.organizers,
                        eventVM.description,
                        eventVM.challenges,
                        eventVM.location,
                        eventVM.startEvent,
                        eventVM.endEvent,
                        eventVM.guests
                    ),
                    getString(R.string.msgDialog_createdEvent)
                ) {
                    globalEventsVM.processState = State.SUCCESS
                }

                findNavController().navigate(
                    EventFormFragmentDirections.toFragmentExplorer()
                )
            }
        }
    }

    override fun onClickAccept(dialog: DialogFragment) {
        if (isEdited) {
            binding.layoutRvChallenges.rvChallenges.adapter?.notifyItemChanged(challengePos)
            isEdited = false
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
        saveTmpEventChanges()
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

    /**
     * It saves the actual state of the Event form
     * */
    private fun saveTmpEventChanges() {
        //TODO: Complete with the organizers
        eventVM.name = binding.txteEventName.text.toString()
        eventVM.description = binding.txteEventDescription.text.toString()
        locationChosen?.let {
            eventVM.location = Location(
                GeoPoint(it.position.latitude, it.position.longitude),
                locationChosen?.name ?: ""
            )
        }
        eventVM.startEvent
        eventVM.endEvent
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        androidLocationProvider = AndroidLocationProvider(
            requireContext()
        )

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            androidLocationProvider.enable()
        } else {
            val locationPermissionRequest = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                var isGranted =
                    permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
                isGranted = isGranted && permissions.getOrDefault(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    false
                )

                if (isGranted) {
                    androidLocationProvider.enable()
                }
            }

            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}