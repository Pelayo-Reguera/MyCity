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
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
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
import my.city.database.RemoteDatabase
import my.city.databinding.FragmentEventBinding
import my.city.logic.viewmodels.EventVM
import my.city.logic.viewmodels.EventsListVM
import my.city.logic.viewmodels.UserVM
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
    private val userVM: UserVM by activityViewModels()
    private val args: EventFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use the static method from the class binding .bind(<<view>>) when the view is already
        // inflated and we want again an instance of the class binding
        binding = FragmentEventBinding.bind(view)
        binding.vpContent.adapter = adapter

        lifecycleScope.launch {
            eventsListVM.events.value?.let {
                val e = it.find { event -> event.id == args.eventID }
                e?.let { event -> eventVM.event.value = event }
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
            } else {
                context?.let { it1 ->
                    getDrawable(it1, R.drawable.img_default_party)?.let { drawable ->
                        binding.rvEventImages.adapter = EventImagesAdapter(listOf(drawable))
                    }
                }
            }
            //INFO: See to make a toolbar transparent
//            findNavController().currentDestination?.label = eventVM.event.value?.name
        }
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvEventImages)

        if (userVM.isAnonymous.value == false) {
            binding.fabJoin.isEnabled = true
            eventVM.event.value?.let {
                if (it.isUserJoined) {
                    context?.let { it1 ->
                        binding.fabJoin.setImageDrawable(
                            getDrawable(
                                it1,
                                R.drawable.blank_baseline_celebration_24
                            )
                        )
                    }
                }
            }
            binding.fabJoin.setOnClickListener {
                eventVM.event.value?.let {
                    userVM.userName.value?.let { username ->
                        if (userVM.isConnected) {
                            if (it.isUserJoined) {
                                RemoteDatabase.disjoinEvent(it.id, username, {
                                    it.isUserJoined = !it.isUserJoined
                                    context?.let { it1 ->
                                        binding.fabJoin.setImageDrawable(
                                            getDrawable(it1, R.drawable.outline_celebration_30)
                                        )
                                    }
                                }, {})

                            } else {
                                RemoteDatabase.joinEvent(it.id, username, {
                                    it.isUserJoined = !it.isUserJoined
                                    context?.let { it1 ->
                                        binding.fabJoin.setImageDrawable(
                                            getDrawable(
                                                it1,
                                                R.drawable.blank_baseline_celebration_24
                                            )
                                        )
                                    }
                                }, {})
                            }
                        } else {
                            Toast.makeText(
                                context,
                                getString(R.string.internet_connection_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}