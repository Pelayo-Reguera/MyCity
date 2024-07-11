/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.profile

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import my.city.R
import my.city.databinding.FragmentProfileBinding
import my.city.logic.viewmodels.UserVM

/**
 * A simple [Fragment] subclass for showing user profile information
 * */

//The XMl of the layout is passed as an argument to the Fragment's constructor to create it and
// attach it to the parent (in this case the content of MainActivity) to do more things during
// this process, override the method onCreateView (which returns "return inflater.inflate(R.layout
// .<<corresponding layout>>, container, false)")
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

    // The ViewModel of its activity container is retrieved. In this case the UserVM
    private val userVm: UserVM by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentProfileBinding.bind(view)
        userVm.userName.observe(viewLifecycleOwner) {
            if (!userVm.isAnonymous.value!!) {
                binding.txtUsername.text = userVm.userName.value
            }
        }

        userVm.profilePhoto.observe(viewLifecycleOwner) {
            if (!userVm.isAnonymous.value!!) {
                userVm.profilePhoto.value?.let {
                    binding.imgProfile.setImageDrawable(it)
                }
            }
        }
        configureMenu()
    }

    private fun configureMenu() {
        activity?.let {
            val menuHost: MenuHost = it
            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // Infla el menú específico para este fragmento
                    menuInflater.inflate(R.menu.profile_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    // Maneja las acciones del menú aquí
                    return when (menuItem.itemId) {
                        R.id.item_signOut -> {
                            userVm.signOut()
                            true
                        }

                        R.id.item_settings -> {
                            //TODO: Do something
                            Toast.makeText(context, "Settings", Toast.LENGTH_LONG).show()
                            true
                        }

                        else -> false
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }
    }
}