/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic

import android.graphics.drawable.Drawable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import java.time.LocalDateTime
import java.time.ZoneId

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
 * @property name The name of the event
 * @property eventDrawables [MutableList] of drawables of the Event
 * @property eventImgURIs [MutableList] with paths to each image of the Event
 * @property organizers [MutableList] with the name of the users who organize this Event
 * @property description Details of the Event
 * @property challenges [MutableList] containing all activities that attendees can do during the event
 * @property location [GeoPoint] of the place where the event is held
 * @property street The address of the Event
 * @property startEvent [Timestamp] representing the date and hour when the event begins.
 * The secondary constructor automatically converts from [LocalDateTime] to [Timestamp]
 * @property endEvent [Timestamp] representing the date and hour when the event ends.
 * The secondary constructor automatically converts from [LocalDateTime] to [Timestamp]
 * @property guests [MutableList] containing all the users that will attend this event
 *
 * @author Pelayo Reguera García
 * */
data class Event(,
    var eventImgURIs: MutableList<String> = mutableListOf(),
    var organizers: MutableList<String> = mutableListOf(),
) {

    @Exclude
    var id: String = ""
        @Exclude get
        @Exclude set
    lateinit var name: String

    @Exclude
    var eventDrawables: MutableList<Drawable> = mutableListOf()
        @Exclude get
        @Exclude set
    lateinit var description: String

    @Exclude
    var challenges: MutableList<Challenge> = mutableListOf()
        @Exclude get
        @Exclude set
    lateinit var location: GeoPoint
    lateinit var street: String
    lateinit var startEvent: Timestamp
    lateinit var endEvent: Timestamp

    @Exclude
    var guests: MutableList<User> = mutableListOf()
        @Exclude get
        @Exclude set

    constructor(
        name: String,
        eventDrawables: MutableList<Drawable>,
        eventImgURIs: MutableList<String>,
        organizers: MutableList<String>,
        description: String,
        challenges: MutableList<Challenge>,
        location: GeoPoint,
        street: String,
        startEvent: LocalDateTime,
        endEvent: LocalDateTime,
        guests: MutableList<User>,
        id: String = "", //It's the last one to avoid change all the calls to this constructor
    ) : this(eventImgURIs, organizers) {
        this.id = id
        this.name = name
        this.eventDrawables = eventDrawables
        this.description = description
        this.challenges = challenges
        this.location = location
        this.street = street

        var instant = startEvent.atZone(ZoneId.systemDefault()).toInstant()
        this.startEvent = Timestamp(instant.epochSecond, instant.nano)

        instant = endEvent.atZone(ZoneId.systemDefault()).toInstant()
        this.endEvent = Timestamp(instant.epochSecond, instant.nano)
        this.guests = guests
    }

    fun copy(): Event {
        return Event(
            name,
            eventDrawables.toMutableList(),
            eventImgURIs.toMutableList(),
            organizers.toMutableList(),
            description,
            challenges,
            GeoPoint(location.latitude, location.longitude),
            street,
            startEvent.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            endEvent.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            guests,
            id
        )
    }

    fun startLocalDateTime(): LocalDateTime {
        return startEvent.toDate().toInstant().atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    fun endLocalDateTime(): LocalDateTime {
        return endEvent.toDate().toInstant().atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
}

/**
 * It represents an User with all the information attached to it
 *
 * @property isAnonymous
 * @property userName
 * @property email
 * @property photoUrl
 * @property likedEvents A [MutableList] containing all the events that the user like or is register
 * as an attendant
 * @property coins A [MutableList] containing the quantities of each coin type in the user's possession
 *
 * @author Pelayo Reguera García
 * */
data class User(
    val userName: String,
    val email: String,
    val photoUrl: String,
    val likedEvents: MutableList<Event>,
    val coins: MutableMap<String, Int>,
    val isAnonymous: Boolean = false,
)