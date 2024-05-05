/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val user: UserVM by lazy { UserVM() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentProfileBinding.bind(view)
        lifecycleScope.launch(Dispatchers.IO) {
            binding.txtUsername.text = user.getUser()
        }
    }
}