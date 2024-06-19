/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.toObject
import my.city.database.RemoteDatabase
import my.city.database.Tags
import my.city.logic.Event
import my.city.logic.Location
import my.city.logic.User
import java.time.LocalDateTime

enum class State {
    IN_PROCESS, SUCCESS, FAILURE
}

class EventsListVM : ViewModel() {

    val events: MutableLiveData<MutableList<Event>> = MutableLiveData(mutableListOf())
        get() {
            RemoteDatabase.getEvents({
                val list: MutableList<Event> = field.value ?: mutableListOf()
                for (document in it.documents) {
                    document.toObject<Event>()?.let { it1 -> list.add(it1) }
                }
                field.value = list
            }, {
                Log.w(Tags.REMOTE_DATABASE_ERROR.toString(), it.message.toString())
            })
            return field
        }

    var processState: State = State.IN_PROCESS

    /** Message to be shown when any change has been done*/
    val message: MutableLiveData<String> = MutableLiveData()

    /**
     *  Saving an [Event] to the database
     *
     *  @param event The Event to be stored
     *  @param message The message to show to the user if desired
     *  @param onSuccess Actions to do in case the event was successfully stored
     * */
    fun addEvent(event: Event, message: String, onSuccess: () -> Unit) {
        val list = events.value ?: mutableListOf()
        //FIXME: Delete this event and use the parameter
        val event = Event(
            "c",
            mutableListOf(
                User(
                    "User1",
                    "email1@gmail.com",
                    "photoURL",
                    mutableListOf(),
                    mutableMapOf()
                )
            ),
            "Evento de ejemplo",
            mutableListOf(),
            Location(GeoPoint(0.0, 0.0), "Mi casa"),
            LocalDateTime.now(),
            LocalDateTime.now(),
            mutableListOf(),
        )
        list.add(event)
        this.message.value = message
        events.value = list
        processState = State.IN_PROCESS

        RemoteDatabase.createEvent(event, onSuccess) {
            Log.w(Tags.REMOTE_DATABASE_ERROR.toString(), it.message.toString())
        }
    }
}