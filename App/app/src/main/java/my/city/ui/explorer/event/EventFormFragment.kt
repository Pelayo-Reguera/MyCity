/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.MultiBrowseCarouselStrategy
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
import my.city.logic.viewmodels.EventCreationVM
import my.city.logic.viewmodels.EventsListVM
import my.city.ui.explorer.event.challenges.EventChallengesAdapter
import my.city.util.DialogListeners
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * This class sets the logic for the [Event Form][R.id.fragment_event_form] where the user can
 * create its own event with its own challenges.
 * */
class EventFormFragment : Fragment(R.layout.fragment_event_form), DialogListeners {

    /** Used to know the current position of the user for better results in locations search*/
    private lateinit var androidLocationProvider: LocationProvider

    /** [EventCreationVM] instance for storing the information of the event being created*/
    private val eventVM: EventCreationVM by navGraphViewModels(R.id.event_form_navigation)
    private val eventsListVM: EventsListVM by activityViewModels()

    private lateinit var binding: FragmentEventFormBinding
    private val rvEventImages: RecyclerView by lazy { binding.rvEventImages }
    private val txtEventName: TextInputLayout by lazy { binding.txtEventName }
    private val txteEventName: TextInputEditText by lazy { binding.txteEventName }
    private val txtEventDescription: TextInputLayout by lazy { binding.txtEventDescription }
    private val txteEventDescription: TextInputEditText by lazy { binding.txteEventDescription }
    private val txtEventLocation: TextInputLayout by lazy { binding.txtEventLocation }
    private val txteEventLocation: TextInputEditText by lazy { binding.txteEventLocation }
    private val txtEventStartDate: TextInputLayout by lazy { binding.txtEventStartDate }
    private val txteEventStartDate: TextInputEditText by lazy { binding.txteEventStartDate }
    private val txtEventEndDate: TextInputLayout by lazy { binding.txtEventEndDate }
    private val txteEventEndDate: TextInputEditText by lazy { binding.txteEventEndDate }

    private var isEdited: Boolean = false
    private var challengePos: Int = 0
    private var imgEventPos: Int = 0
    private val pickMedia by lazy {
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { it1 ->
            context?.let {
                for (uri in it1) {
                    val request = ImageRequest.Builder(it)
                        .data(uri)
                        .target(onSuccess = { it1 ->
                            eventVM.eventDrawables.add(it1)
                            eventVM.eventImgURIs.add(uri.toString())
                            rvEventImages.adapter?.notifyItemInserted(
                                eventVM.eventDrawables.size - 1
                            )
                        })
                        .build()
                    it.imageLoader.enqueue(request)
                }
            }
        }
    }
    private val onlineSearch: Search by lazy {
        OnlineSearch.create(
            requireContext(),
            "De8ig7uYydhNPpGTnNS0PMqAQxagES3R" //TODO: Remove
        )
    }

    /** This property is used for performance purposes with the TomTom's API*/
    private var isSearching = false

    /** It determines whether the device is a smartphone or other bigger*/
    private val isLargeLayout: Boolean by lazy { resources.getBoolean(R.bool.large_layout) }

    //FIXME: Set a new field for the maximum number of guests
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventFormBinding.bind(view)
        loadFields()

        val carousel = CarouselLayoutManager()
        carousel.setCarouselStrategy(MultiBrowseCarouselStrategy())
        rvEventImages.layoutManager = carousel
        if (eventVM.eventDrawables.size == 0) {
            context?.let {
                getDrawable(
                    it,
                    R.drawable.add_image
                )?.let { it1 -> eventVM.eventDrawables.add(it1) }
            }
        }
        rvEventImages.adapter = EventImagesAdapter(eventVM.eventDrawables, pickMedia)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(rvEventImages)

        rvEventImages.setOnScrollChangeListener { _, _, _, _, _ ->
            val pos = snapHelper.findSnapView(carousel)?.let { carousel.getPosition(it) }
            pos?.let {
                imgEventPos = it
                binding.btniRemoveImg.isEnabled = it > 0
            }
        }

        binding.btniRemoveImg.setOnClickListener {
            eventVM.eventDrawables.removeAt(imgEventPos)
            eventVM.eventImgURIs.removeAt(imgEventPos)
            rvEventImages.adapter?.notifyItemRemoved(imgEventPos)
        }

        txteEventName.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                eventVM.name = txteEventName.text.toString()
            }
        }

        txteEventDescription.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                eventVM.description = txteEventDescription.text.toString()
            }
        }

        txteEventLocation.doOnTextChanged { text, _, _, _ ->
            eventVM.location = null
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
                            result.results.map {
                                context?.let { it1 ->
                                    it.toPlace(
                                        UnitSystem.Metric,
                                        DistanceFormatter(it1)
                                    )
                                }
                            }.let { binding.searchResultsView.update(it as List<PlaceDetails>) }
                            isSearching = false
                        }
                    })
            }
        }

        txteEventLocation.setOnFocusChangeListener { v, hasFocus ->
            binding.searchResultsView.isVisible = hasFocus
        }

        // When an option is clicked its text is written in the EditText
        binding.searchResultsView.searchResultClickListener = SearchResultClickListener {
            txteEventLocation.setText(it.address)
            eventVM.location = GeoPoint(it.position.latitude, it.position.longitude)
            eventVM.street = it.address
            binding.searchResultsView.visibility = View.GONE
        }

        txteEventStartDate.setOnClickListener { _ ->
            showDateTimePickers { year: Int, month, day, hour, minute ->
                val date = LocalDateTime.of(year, month, day, hour, minute)
                eventVM.startEvent = date
                txteEventStartDate.setText(
                    date.format(DateTimeFormatter.ofPattern("dd/LL/uuuu - HH:mm"))
                )
            }
        }

        txteEventEndDate.setOnClickListener { _ ->
            showDateTimePickers { year: Int, month, day, hour, minute ->
                val date = LocalDateTime.of(year, month, day, hour, minute)
                eventVM.endEvent = date
                txteEventEndDate.setText(
                    date.format(DateTimeFormatter.ofPattern("dd/LL/uuuu - HH:mm"))
                )
            }

        }

        binding.btniAddChallenge.setOnClickListener { showChallengeForm() }

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

        binding.efabReady.setOnClickListener {
            val emptyError = getString(R.string.txtEmptyError)
            var isValid = if (txteEventName.text.isNullOrBlank() || eventVM.name.isBlank()) {
                txtEventName.error = emptyError
                false
            } else {
                txtEventName.error = null
                true
            }

            isValid =
                if (txteEventDescription.text.isNullOrBlank() || eventVM.description.isBlank()) {
                    txtEventDescription.error = emptyError
                false
            } else {
                    txtEventDescription.error = null
                isValid
            }

            isValid = if (txteEventLocation.text.isNullOrBlank()) {
                txtEventLocation.error = emptyError
                false
            } else if (eventVM.location == null || eventVM.street.isBlank()) {
                txtEventLocation.error = getString(R.string.location_not_selected)
                false
            } else {
                txtEventLocation.error = null
                isValid
            }

            isValid = if (txteEventStartDate.text.isNullOrBlank() || eventVM.endEvent == null) {
                txtEventStartDate.error = emptyError
                false
            } else {
                txtEventStartDate.error = null
                isValid
            }

            isValid = if (txteEventEndDate.text.isNullOrBlank() || eventVM.endEvent == null) {
                txtEventEndDate.error = emptyError
                false
            } else if (eventVM.endEvent!!.isBefore(eventVM.startEvent) ||
                eventVM.endEvent!!.isEqual(eventVM.startEvent)
            ) {
                txtEventEndDate.error =
                    getString(R.string.event_form_txteEventEndDate_error)
                false
            } else {
                txtEventEndDate.error = null
                isValid
            }

            if (isValid) {
                eventsListVM.addEvent(
                    Event(
                        eventVM.name,
                        eventVM.eventDrawables,
                        eventVM.eventImgURIs,
                        mutableListOf(), //TODO: Change
                        eventVM.description,
                        eventVM.challenges,
                        eventVM.location!!,
                        eventVM.street,
                        eventVM.startEvent!!,
                        eventVM.endEvent!!,
                        mutableListOf() //TODO: Change
                    ),
                    getString(R.string.msgDialog_createdEvent)
                ) { /*Do nothing*/ }

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
     * It creates a [DatePickerDialog] and if a date is chosen then [TimePickerDialog] is called
     *
     * @param saveDateTime Actions to correctly save the DateTime
     * */
    private fun showDateTimePickers(saveDateTime: (Int, Int, Int, Int, Int) -> Unit) {
        context?.let {
            val calendar: Calendar = Calendar.getInstance()
            val dateDialog = DatePickerDialog(
                it,
                { _, year, month, dayOfMonth ->
                    TimePickerDialog(
                        it,
                        { _, hourOfDay, minute ->
                            saveDateTime(year, month, dayOfMonth, hourOfDay, minute)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            )
            dateDialog.datePicker.minDate = calendar.timeInMillis
            dateDialog.show()
        }
    }

    /**
     * Fills the form with the temporal data stored in the [eventVM]
     * */
    private fun loadFields() {
        txteEventName.setText(eventVM.name)
        txteEventDescription.setText(eventVM.description)
        txteEventLocation.setText(eventVM.street)
        eventVM.startEvent?.let {
            txteEventStartDate.setText(
                it.format(
                    DateTimeFormatter.ofPattern("dd/LL/uuuu - HH:mm")
                )
            )
        }
        eventVM.endEvent?.let {
            txteEventEndDate.setText(
                it.format(DateTimeFormatter.ofPattern("dd/LL/uuuu - HH:mm"))
            )
        }
    }

    // The permission for accessing user's location is checked
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