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
 * It encapsulates all the information of a challenge created for a specific event
 *
 * @property name The name of the challenge
 * @property description
 * @property coinID The ID of a specific coin with which it is rewarded
 * @property reward The amount of coins that this challenge gives to whoever completes it
 *
 * @author Pelayo Reguera García
 * */
data class Challenge(var reward: Int = 0) {

    lateinit var name: String
    lateinit var description: String
    lateinit var coinID: String

    constructor(name: String, description: String, coinID: String, reward: Int = 0) : this(reward) {
        this.name = name
        this.description = description
        this.coinID = coinID
    }
}

/**
 * It encapsulates all the information of an Event created by an organizer
 *
 * @property id The unique ID of the event in the database
 * @property name The name of the Event
 * @property eventDrawables [MutableList] of drawables of the Event
 * @property eventImgURIs [MutableList] with paths to each image of the Event
 * @property organizers [MutableList] with the users' names who organize this Event
 * @property description Details of the Event
 * @property challenges [MutableList] containing all activities that attendees can do during the event
 * @property location [GeoPoint] of the place where the event is held
 * @property street The address of the  in text format
 * @property startEvent [Timestamp] representing the date and hour when the event begins.
 * The secondary constructor automatically converts from [LocalDateTime] to [Timestamp]
 * @property endEvent [Timestamp] representing the date and hour when the event ends.
 * The secondary constructor automatically converts from [LocalDateTime] to [Timestamp]
 * @property guestsCapacity The maximum number of people that can attend to the Event
 * @property guestsNJoined The current number of people joined to the Event
 * @property guestsUserNames [MutableList] containing all the users that will attend this event
 *
 * @author Pelayo Reguera García
 * */
data class Event(
    var eventImgURIs: MutableList<String> = mutableListOf(),
    var organizers: MutableList<String> = mutableListOf(),
    var guestsCapacity: Int = 10,
    var guestsNJoined: Int = 0,
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
    var guestsUserNames: MutableList<String> = mutableListOf()
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
        guestsUserNames: MutableList<String> = mutableListOf(),
        guestsCapacity: Int = 10,
        guestsNJoined: Int = 0,
        id: String = "", //It's the last one to avoid change all the calls to this constructor
    ) : this(eventImgURIs, organizers, guestsCapacity, guestsNJoined) {
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
        this.guestsUserNames = guestsUserNames
    }

    /**
     * It creates an exact copy of the Event
     *
     * @return An [Event] object
     * */
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
            getStartLocalDateTime(),
            getEndLocalDateTime(),
            guestsUserNames,
            guestsCapacity,
            guestsNJoined,
            id
        )
    }

    /**
     * Transforms the [startEvent] in [Timestamp] format to [LocalDateTime] format
     *
     * @return [LocalDateTime] object
     * */
    @Exclude
    fun getStartLocalDateTime(): LocalDateTime {
        return startEvent.toDate().toInstant().atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    /**
     * Transforms the [endEvent] in [Timestamp] format to [LocalDateTime] format
     *
     * @return [LocalDateTime] object
     * */
    @Exclude
    fun getEndLocalDateTime(): LocalDateTime {
        return endEvent.toDate().toInstant().atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
}

/**
 * It encapsulates all the information of an User (normally the one using the app)
 *
 * @property name
 * @property email
 * @property location The default location set when the User created its account
 * @property birthdate
 * @property gender
 * @property friends [MutableList] of pair values with the usernames of friends as key
 * and their profile photo as a [Drawable]
 * @property createdEvents
 * @property likedEvents [MutableList] containing all the events that the user like
 * @property joinedEvents [MutableList] containing all the events that the user is registered
 * @property coins [MutableList] containing the quantities of each coin type the user has
 *
 * @author Pelayo Reguera García
 * */
data class User(
    val friends: MutableList<Pair<String, Drawable>> = mutableListOf(),
    val createdEvents: MutableList<Event> = mutableListOf(),
    val likedEvents: MutableList<Event> = mutableListOf(),
    val joinedEvents: MutableList<Event> = mutableListOf(),
    val coins: MutableMap<String, Int> = mutableMapOf(),
    var isAnonymous: Boolean = false,
) {

    var name: String = ""
    var email: String = ""
    var location: GeoPoint = GeoPoint(0.0, 0.0)
    var birthdate: Timestamp = Timestamp.now()
    var gender: String = ""

    /**
     * It is for when it is needed to create an User object manually
     * */
    constructor(
        name: String,
        email: String,
        location: com.tomtom.sdk.location.GeoPoint,
        friends: MutableList<Pair<String, Drawable>> = mutableListOf(),
        createdEvents: MutableList<Event> = mutableListOf(),
        likedEvents: MutableList<Event> = mutableListOf(),
        joinedEvents: MutableList<Event> = mutableListOf(),
        coins: MutableMap<String, Int> = mutableMapOf(),
        isAnonymous: Boolean = false,
    ) : this(friends, createdEvents, likedEvents, joinedEvents, coins, isAnonymous) {
        this.name = name
        this.email = email
        this.location = GeoPoint(location.latitude, location.longitude)
    }
}