/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import my.city.R
import my.city.databinding.FragmentEventBinding
import my.city.ui.explorer.event.challenges.EventChallengesFragment

class EventFragment : Fragment(R.layout.fragment_event) {
    class EventInfoAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) EventInfoFragment() else EventChallengesFragment()
        }
    }

    private lateinit var binding: FragmentEventBinding
    private val adapter: EventInfoAdapter by lazy { EventInfoAdapter(this) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventBinding.bind(view)
        binding.vpContent.adapter = adapter
        TabLayoutMediator(binding.tabsEvent, binding.vpContent) { tab, position ->
            if (position == 0) {
                tab.text = getString(R.string.tabiInfo)
                tab.icon =
                    context?.let { ContextCompat.getDrawable(it, R.drawable.baseline_info_24) }
            } else {
                tab.text = getString(R.string.tabiChallenge)
                tab.icon = context?.let {
                    ContextCompat.getDrawable(
                        it,
                        R.drawable.baseline_emoji_events_24
                    )
                }
            }
        }.attach()
        activity?.actionBar?.hide()
    }
}