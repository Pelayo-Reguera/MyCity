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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.MultiBrowseCarouselStrategy
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import my.city.R
import my.city.databinding.FragmentEventBinding
import my.city.logic.viewmodels.EventVM
import my.city.logic.viewmodels.EventsListVM
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
    private val eventsListVM: EventsListVM by activityViewModels()
    private val eventVM: EventVM by navGraphViewModels(R.id.event_navigation)
    private val args: EventFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventBinding.bind(view)
        binding.vpContent.adapter = adapter

        lifecycleScope.launch {
            eventsListVM.events.value?.let {
                for (event in it) {
                    if (event.id == args.eventID) {
                        //TODO: Add animation to the image and keep empty the rest of fields
                        eventVM.event.value = event
                        break
                    }
                }
            }
        }

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


        val carousel = CarouselLayoutManager()
        carousel.setCarouselStrategy(MultiBrowseCarouselStrategy())
        binding.rvEventImages.layoutManager = carousel
        eventVM.event.observe(viewLifecycleOwner) {
            if (it.eventDrawables.size > 0) {
                binding.rvEventImages.adapter = EventImagesAdapter(it.eventDrawables)
            }
            //INFO: See to make a toolbar transparent
//            findNavController().currentDestination?.label = eventVM.event.value?.name
        }
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvEventImages)
        binding.fabJoin.setOnClickListener { /*TODO: Subscribe to the Event*/ }
    }
}