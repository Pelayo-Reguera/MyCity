/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic

import com.google.firebase.firestore.GeoPoint
import java.util.Date

/**
 * It represents all the information a challenge created for a specific event can contain
 *
 * @property name The name of the challenge
 * @property description
 * @property coinID The ID of a specific coin
 * @property reward The amount of coins that this challenge gives to whoever completes it
 *
 * @author Pelayo Reguera García
 * */
data class Challenge(val name: String, val description: String, val coinID: String, val reward: Int)

/**
 * It represents all the information an event created by an organizer can contain
 *
 * @property organizer The name of the user who organizes this event
 * @property title The name of the event
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
    val title: String,
    val organizers: MutableList<User>,
    val description: String,
    val challenges: MutableList<Challenge>,
    val location: Location,
    val startEvent: Date,
    val endEvent: Date,
    val guests: MutableList<User>
)
//TODO: Get the data from the database

data class Location(val geoPoint: GeoPoint, val street: String)

/**
 * It represents an User with all the information attached to it
 *
 * @property id
 * @property userName
 * @property likedEvents A [MutableList] containing all the events that the user like or is register
 * as an attendant
 * @property coins A [MutableList] containing the quantities of each coin type in the user's possession
 *
 * @author Pelayo Reguera García
 * */
data class User(
    val userName: String,
    val email: String,
    val likedEvents: MutableList<Event>,
    val coins: MutableMap<String, Int>,
)