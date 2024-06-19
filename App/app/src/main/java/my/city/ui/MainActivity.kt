/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest.Builder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import my.city.R
import my.city.databinding.ActivityMainBinding
import my.city.logic.viewmodels.EventsListVM
import my.city.logic.viewmodels.UserVM

/**
 * The first activity loaded when the app is opened. From here, a user will be able to do most of
 * the things this app allows like:
 * - Search events by name in the [ExplorerRecyclerFragment][my.city.ui.explorer.ExplorerEventsFragment]
 * - See upcoming events around the user in the [MapFragment][my.city.ui.map.MapFragment]
 * - See the user information like the number of MyPoints, followers, name...
 *
 *  @author Pelayo Reguera García
 * */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    // Its initialization is delegated to viewModels() to determine its scope to the activity in this
    // case. It doesn't work if the default constructor of userVM or ViewModel() is used
    private val userVm: UserVM by viewModels()
    private val eventsListVM: EventsListVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        //INFO: Try to improve the splash screen using an animated icon
        installSplashScreen()
        runBlocking {
            launch {
                userVm.signInLauncher = registerForActivityResult(
                    FirebaseAuthUIActivityResultContract(),
                ) { res ->
                    userVm.onSignInResult(res)
                }
            }
            delay(1000)
        }

        super.onCreate(savedInstanceState)
        //It creates an instance of the binding class between this class and the corresponding xml file
        binding = ActivityMainBinding.inflate(layoutInflater)
        //root is the outermost layout of the referenced xml
        setContentView(binding.root)

        // We are setting the toolbar widget as the application action replacing the one that would
        // be predefined by themes if we had not written ".NoActionBar" at the end
        setSupportActionBar(binding.toolbar)

        // It returns the navigation controller with the navigation graph ("main_navigation.xml") from
        // the NavHostFragment
        navController =
            (supportFragmentManager.findFragmentById(R.id.content_activity_main) as NavHostFragment).navController
        // Instead of setting the navigation graph to the action bar (the toolbar in this case)
        // we are setting the three fragments that will be the main ones of the app in order to
        // avoid showing the NavigateUp arrow in them
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.fragment_events_explorer,
                R.id.fragment_map,
                R.id.fragment_profile
            )
        )
        val navView: BottomNavigationView = binding.navView
        // appBarConfiguration is added in order to determine when the NavigateUp arrow should be
        // displayed
        setupActionBarWithNavController(navController, appBarConfiguration)
        // The navView is also configured with the navController to show the fragment corresponding
        // to the icon pressed
        navView.setupWithNavController(navController)
    }

    override fun onStart() {
        super.onStart()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CHANGE_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_SETTINGS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            internetConfig()
        } else {
            val locationPermissionRequest = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                var isGranted =
                    permissions.getOrDefault(Manifest.permission.CHANGE_NETWORK_STATE, false)
                isGranted = isGranted && permissions.getOrDefault(
                    Manifest.permission.WRITE_SETTINGS,
                    false
                )

                if (isGranted) {
                    internetConfig()
                }
            }

            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.WRITE_SETTINGS
                )
            )
        }

        userVm.signIn()
    }

    private fun internetConfig() {
        val connectivityManager = ContextCompat.getSystemService(
            this,
            ConnectivityManager::class.java
        ) as ConnectivityManager

        val networkRequest = Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // network is available for use
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                userVm.isConnected = true
            }

            // Network capabilities have changed for the network
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                userVm.isConnected =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (!userVm.isConnected) {
                    Toast.makeText(
                        this@MainActivity,
                        R.string.internet_connection_error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // lost network connection
            override fun onLost(network: Network) {
                super.onLost(network)
                userVm.isConnected = false
                Toast.makeText(
                    this@MainActivity,
                    R.string.internet_connection_error,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}