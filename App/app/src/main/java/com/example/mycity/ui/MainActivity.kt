package com.example.mycity.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mycity.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

/*
* MyCity © 2024 by Pelayo Reguera García is licensed under
* Attribution-NonCommercial-NoDerivatives 4.0 International.
*
* To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
* */

/**
 *
 *  @author Pelayo Reguera García
 * */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
        val navController = binding.contentActivityMain.findNavController()
        // We are setting the navigation graph to the action bar (the toolbar in this case)
        // display correctly the titles and more things
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val navView: BottomNavigationView = binding.navView
        // appBarConfiguration is added in order to determine when the NavigateUp arrow should be
        // displayed
        setupActionBarWithNavController(navController, appBarConfiguration)
        // The navVew is also configured with the navController to show the fragment corresponding
        // to the icon pressed
        navView.setupWithNavController(navController)
    }
}