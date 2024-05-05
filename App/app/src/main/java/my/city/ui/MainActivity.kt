/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import my.city.R
import my.city.databinding.ActivityMainBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
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
                R.id.fragment_explorer,
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

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}