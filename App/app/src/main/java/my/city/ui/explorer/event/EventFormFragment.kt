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
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.icu.util.Calendar
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.PagerSnapHelper
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.MultiBrowseCarouselStrategy
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.city.R
import my.city.database.FilterPattern
import my.city.databinding.FragmentEventFormBinding
import my.city.logic.Event
import my.city.logic.viewmodels.EventCreationVM
import my.city.logic.viewmodels.EventsListVM
import my.city.logic.viewmodels.UserVM
import my.city.ui.explorer.event.challenges.EventChallengesAdapter
import my.city.util.DialogListeners
import my.city.util.PatternInputFilter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * This class sets the logic for the [Event Form][R.layout.fragment_event_form] where the user can
 * create its own event with its own challenges.
 *
 * @author Pelayo Reguera García
 * */
class EventFormFragment : Fragment(R.layout.fragment_event_form), DialogListeners {

    /** Used to know the current position of the user for better results in locations search*/
    private lateinit var androidLocationProvider: LocationProvider
    private val maxNPhotos = 10
    private val userVm: UserVM by activityViewModels()

    /** [EventCreationVM] instance for storing the information of the event being created*/
    private val eventVM: EventCreationVM by navGraphViewModels(R.id.event_form_navigation)

    /** [EventsListVM] instance for storing the information of this event when it's finally created*/
    private val eventsListVM: EventsListVM by activityViewModels()

    private lateinit var binding: FragmentEventFormBinding
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
    private val txtEventNGuests: TextInputLayout by lazy { binding.txtEventNGuests }
    private val txteEventNGuests: TextInputEditText by lazy { binding.txteEventNGuests }

    private var isEdited: Boolean = false
    private var challengePos: Int = 0
    private var imgEventPos: Int = 0
    private val itemsAdapter: ArrayAdapter<String> by lazy {
        ArrayAdapter<String>(
            requireContext(),
            R.layout.txt_autocomplete_item,
            listOf()
        )
    }
    private val pickMedia: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxNPhotos)) { it1 ->
            context?.let {
                for (uri in it1) {
                    val request = ImageRequest.Builder(it)
                        .data(uri)
                        .target(onSuccess = { it1 ->
                            eventVM.eventDrawables.add(it1)
                            eventVM.eventImgURIs.add(uri.toString())
                            binding.rvEventImages.adapter?.notifyItemInserted(
                                eventVM.eventDrawables.size - 1
                            )
                        })
                        .build()
                    it.imageLoader.enqueue(request)
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

    /** Informs whether there is a new input to consume in a search*/
    private var requestSearch: Boolean = false

    /** It determines whether the device is a smartphone or other bigger*/
    private val isLargeLayout: Boolean by lazy { resources.getBoolean(R.bool.large_layout) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventFormBinding.bind(view)
        loadFields()

        val carousel = CarouselLayoutManager()
        carousel.setCarouselStrategy(MultiBrowseCarouselStrategy())
        binding.rvEventImages.layoutManager = carousel
        if (eventVM.eventDrawables.size == 0) {
            context?.let {
                getDrawable(
                    it,
                    R.drawable.add_image
                )?.let { it1 -> eventVM.eventDrawables.add(it1) }
            }
        }
        binding.rvEventImages.adapter = null
        binding.rvEventImages.adapter = EventImagesAdapter(eventVM.eventDrawables, pickMedia)

        val snapHelper = PagerSnapHelper()
        binding.rvEventImages.onFlingListener = null
        snapHelper.attachToRecyclerView(binding.rvEventImages)

        binding.rvEventImages.setOnScrollChangeListener { _, _, _, _, _ ->
            val pos = snapHelper.findSnapView(carousel)?.let { carousel.getPosition(it) }
            pos?.let {
                imgEventPos = it
                binding.btniRemoveImg.isEnabled = it > 0
            }
        }

        binding.btniRemoveImg.setOnClickListener {
            eventVM.eventDrawables.removeAt(imgEventPos)
            eventVM.eventImgURIs.removeAt(imgEventPos)
            binding.rvEventImages.adapter?.notifyItemRemoved(imgEventPos)
        }

        txteEventName.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                eventVM.name = txteEventName.text.toString()
            }
        }

        // It doesn't work with lazy { }
        binding.txteEventOrganizers.filters = arrayOf(PatternInputFilter(FilterPattern.USER_NAME))
        binding.txteEventOrganizers.setAdapter(itemsAdapter)

        binding.txteEventOrganizers.doOnTextChanged { text, _, _, _ ->
            eventVM.requestSearch = !text.isNullOrBlank()
            if (!eventVM.isSearching) {
                lifecycleScope.launch(Dispatchers.Default) {
                    makeFindUserRequest()
                }
            }
        }

        binding.txteEventOrganizers.setOnItemClickListener { _, view, _, _ ->
            val selection: MaterialTextView = view as MaterialTextView
            eventVM.addOrganizer(selection.text.toString())
            chipCreation(selection.text.toString(), true)
            // The text should be cleared
            binding.txteEventOrganizers.setText("")
            //The options should be remove in order to avoid to show the same user names again
            (binding.txteEventOrganizers.adapter as ArrayAdapter<*>).clear()
        }

        txteEventDescription.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                eventVM.description = txteEventDescription.text.toString()
            }
        }

        txteEventLocation.doOnTextChanged { text, _, _, _ ->
            eventVM.location = null
            requestSearch = !text.isNullOrBlank()
            // This 'if' avoids to overload with many requests
            if (!isSearching) {
                lifecycleScope.launch(Dispatchers.Default) {
                    makeSearchRequest()
                }
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

        binding.txteEventNGuests.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                eventVM.guestsCapacity = txteEventNGuests.text.toString().toInt()
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

            isValid = if (txteEventStartDate.text.isNullOrBlank() || eventVM.startEvent == null) {
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

            isValid = if (txteEventNGuests.text.isNullOrBlank()) {
                txtEventNGuests.error = emptyError
                false
            } else if (eventVM.guestsCapacity < resources.getInteger(R.integer.min_guests_capacity)) {
                txtEventNGuests.error = getString(R.string.event_form_txteEventNGuests_error)
                false
            } else {
                txtEventNGuests.error = null
                isValid
            }

            if (isValid) {
                if (userVm.isConnected) {
                    // The default 'add image' is removed from the list of drawables to not show it as an
                    // image of the Event. This only affects when being showed from the local memory, in
                    // the remote database everything is correct because it depends on eventVM.eventImgURIs
                    // not in the eventVM.eventDrawables
                    eventVM.eventDrawables.removeAt(0)
                    eventsListVM.addEvent(
                        Event(
                            eventVM.name,
                            eventVM.eventDrawables,
                            eventVM.eventImgURIs,
                            eventVM.organizers.keys.mapIndexed { index, s ->
                                if (index == 0) {
                                    userVm.currentUser.value?.displayName.toString()
                                } else {
                                    eventVM.usersFound[s].toString()
                                }
                            }
                                .toMutableList(),
                            eventVM.description,
                            eventVM.challenges,
                            eventVM.location!!,
                            eventVM.street,
                            eventVM.startEvent!!,
                            eventVM.endEvent!!,
                            guestsCapacity = eventVM.guestsCapacity
                        ),
                        getString(R.string.msgDialog_createdEvent)
                    ) { /*Do nothing*/ } //INFO: Improve with local memory persistence

                    findNavController().navigate(
                        EventFormFragmentDirections.toFragmentExplorer()
                    )
                } else {
                    Toast.makeText(context, R.string.internet_connection_error, Toast.LENGTH_SHORT)
                        .show()
                }
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
     *  It requests to [eventVM] to find a user given its username. It also controls the flow with
     *  [EventCreationVM.requestSearch] and [EventCreationVM.isSearching] to avoid overloading the
     *  database
     * */
    private fun makeFindUserRequest() {
        val text = binding.txteEventOrganizers.text
        eventVM.requestSearch = false
        eventVM.isSearching = true
        if (!text.isNullOrBlank()) {
            eventVM.findPossibleUser(text.toString()) {
                requireActivity().runOnUiThread {
                    binding.txteEventOrganizers.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            R.layout.txt_autocomplete_item,
                            it
                        )
                    )
                }
                if (requestSearch) {
                    makeFindUserRequest()
                } else {
                    eventVM.isSearching = false
                }
            }
        } else {
            eventVM.isSearching = false
        }
    }

    /**
     * It makes a search request for possible places given a input introduce by the user.
     * It also controls the flow with [requestSearch] and [isSearching] to avoid overloading
     * the TomTom's API
     * */
    private fun makeSearchRequest() {
        isSearching = true
        requestSearch = false
        val text = binding.txteEventLocation.text
        // The text need to be checked again because it could be modified since the call to itself
        // in the callback
        if (!text.isNullOrBlank()) {
            onlineSearch.search(
                SearchOptions(
                    text.toString(),
                    limit = 5,
                    geoBias = androidLocationProvider.lastKnownLocation?.position ?: userVm.location
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
        if (eventVM.organizers.size == 0) {
            userVm.userName.value?.let { eventVM.organizers[it] = userVm.profilePhoto.value }
        }
        eventVM.organizers.onEachIndexed { index, entry ->
            chipCreation(entry.key, index > 0)
        }
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
        txteEventNGuests.setText(eventVM.guestsCapacity.toString())
    }

    /**
     * Creates a visual [Chip] to display a username and its profile photo if it has one
     *
     * @param userName
     * @param isRemovable It determines if the chip should display a remove button
     * */
    private fun chipCreation(userName: String, isRemovable: Boolean) {
        val chip = Chip(context)
        chip.chipMinHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics
        )
        chip.isCheckable = false
        chip.chipCornerRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics
        )
        chip.chipBackgroundColor =
            context?.let { ColorStateList.valueOf(it.getColor(R.color.brown_transparent)) }
        chip.chipIcon =
            getDrawable(
                requireContext(),
                R.drawable.default_profile
            )?.let { getRoundedBitmap(it) }

        chip.chipIconSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 36f, resources.displayMetrics
        )
        chip.text = userName
        eventVM.organizers[userName]?.let { chip.chipIcon = getRoundedBitmap(it) }

        chip.isCloseIconVisible = if (isRemovable) {
            chip.setOnCloseIconClickListener {
                eventVM.organizers.remove(chip.text)
                binding.chipGroup.removeView(it)
            }
            true
        } else {
            false
        }

        binding.chipGroup.addView(chip)
    }

    /**
     * Transforms the given drawable in a circle centered in the image given
     *
     * @param drawable
     *
     * @return [BitmapDrawable] with the drawable transformed into a circle
     * */
    private fun getRoundedBitmap(drawable: Drawable): BitmapDrawable {
        val bitmap = (drawable as BitmapDrawable).bitmap
        val squareSize = bitmap.width.coerceAtMost(bitmap.height)
        val width = bitmap.width - squareSize
        val height = bitmap.height - squareSize
        val radius = squareSize / 2

        val squaredBitmap =
            Bitmap.createBitmap(bitmap, width / 2, height / 2, squareSize, squareSize)

        val output = Bitmap.createBitmap(squareSize, squareSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        canvas.drawCircle(
            squareSize / 2f,
            squareSize / 2f,
            radius.toFloat(),
            paint
        )

        return BitmapDrawable(resources, output)
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