/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.location.android.AndroidLocationProvider
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.style.LoadingStyleFailure
import com.tomtom.sdk.map.display.style.StandardStyles
import com.tomtom.sdk.map.display.style.StyleLoadingCallback
import com.tomtom.sdk.map.display.ui.MapFragment
import my.city.R
import my.city.database.Tags
import my.city.databinding.FragmentMapBinding
import my.city.logic.viewmodels.EventsListVM
import my.city.logic.viewmodels.UserVM

/**
 * [Fragment] containing a map where events close to a location are shown in pins
 *
 * @author Pelayo Reguera García
 * */
class MapFragment : Fragment(R.layout.fragment_map) {

    /** Used to know the current position of the user for better results in locations search*/
    private lateinit var androidLocationProvider: LocationProvider
    private lateinit var binding: FragmentMapBinding
    private val mapFragment: MapFragment by lazy { binding.mapFragment.getFragment() }
    private val eventsListVM: EventsListVM by activityViewModels()
    private val userVM: UserVM by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentMapBinding.bind(view)

        mapFragment.getMapAsync {
            it.loadStyle(StandardStyles.SATELLITE, object : StyleLoadingCallback {
                override fun onFailure(failure: LoadingStyleFailure) {
                }

                override fun onSuccess() {
                }

            })
            it.setLocationProvider(androidLocationProvider)
            it.enableLocationMarker(LocationMarkerOptions(type = LocationMarkerOptions.Type.Pointer))
            it.moveCamera(
                CameraOptions(
                    position = androidLocationProvider.lastKnownLocation?.position
                        ?: userVM.location,
                    zoom = 10.0
                )
            )
            eventsListVM.events.value?.forEachIndexed { pos, event ->
                it.addMarker(
                    MarkerOptions(
                        coordinate = GeoPoint(
                            event.location.latitude,
                            event.location.longitude
                        ),
                        pinImage = ImageFactory.fromResource(R.drawable.baseline_circle_24),
                        shieldImage = ImageFactory.fromResource(R.drawable.baseline_celebration_24),
                        balloonText = pos.toString()
                    )
                )
            }
            it.addMarkerClickListener { marker ->
                if (marker.isSelected()) {
                    marker.deselect()
                } else {
                    marker.select()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            mapFragment.markerBalloonViewAdapter =
                CustomBalloonViewAdapter(it, eventsListVM.events.value ?: listOf())
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val isLogIn = userVM.getUserData({},
            // In case the user is LogIn but an error arose when retrieving the user data, it redirects
            // to the logIn view
                                         {
                                             when (it) {
                                                 Tags.PROFILE_PHOTO_FAILURE -> {}

                                                 else -> findNavController().navigate(
                                                         MapFragmentDirections.toFragmentLogIn()
                                                 )
                                             }
                                         }
        )
        if (!isLogIn) {
            findNavController().navigate(MapFragmentDirections.toFragmentLogIn())
        } else {
            androidLocationProvider = AndroidLocationProvider(requireContext())

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
                        permissions.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            false
                        )
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
}