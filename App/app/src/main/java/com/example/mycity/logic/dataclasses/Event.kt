package com.example.mycity.logic.dataclasses

import java.util.*

/**
 * It represents all the information an event created by an organizer can contain
 *
 * @property organizer The name of the user who organizes this event
 * @property name The name of the event
 * @property description
 * @property challenges A [MutableList] containing all activities that attendees can do during the event
 * @property location The place where the event is held
 * @property startEvent The date and hour when the event begins
 * @property endEvent The date and hour when the event ends
 * @property guests A [MutableList] containing all the users that will attend this event
 *
 * @author Pelayo Reguera García
 * */
data class Event(
    val organizer: User,
    val name: String,
    val description: String,
    val challenges: MutableList<Challenge>,
    val location: String,
    val startEvent: Date,
    val endEvent: Date,
    val guests: MutableList<User>
)