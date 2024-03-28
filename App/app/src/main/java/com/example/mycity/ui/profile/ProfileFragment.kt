package com.example.mycity.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.mycity.R
import com.example.mycity.databinding.FragmentProfileBinding

/**
 * A simple [Fragment] subclass for showing user profile information
 * */

//The XMl of the layout is passed as an argument to the Fragment's constructor to create it and
// attach it to the parent (in this case the content of MainActivity) to do more things during
// this process, override the method onCreateView (which returns "return inflater.inflate(R.layout
// .<<corresponding layout>>, container, false)")
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentProfileBinding.bind(view)
    }
}