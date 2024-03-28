package com.example.mycity.ui.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.mycity.R
import com.example.mycity.databinding.FragmentMapBinding

/**
 * [Fragment] containing a map where events close to a location are shown in pins
 *
 * @author Pelayo Reguera García
 * */
class MapFragment : Fragment(R.layout.fragment_map) {

    private lateinit var binding: FragmentMapBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentMapBinding.bind(view)
    }
}