/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.city.database.RemoteDatabase
import my.city.database.RemoteStorage
import my.city.logic.Event

enum class State {
    IN_PROCESS, SUCCESS, FAILURE, FINISHED
}

class EventsListVM : ViewModel() {

    //INFO: Try to change it to a a HasMap or LinkedHasMap
    val events: MutableLiveData<MutableList<Event>> = MutableLiveData(mutableListOf())
        get() {
            RemoteDatabase.getEvents(field, { list, event ->
                // Download the images and load them in the Event
                // A coroutine for each event is created to make the loading more dynamic
                viewModelScope.launch {
                    event.eventImgURIs.firstOrNull()?.let { uri ->
                        val segments = Uri.parse(uri).pathSegments
                        val path = segments.joinToString(
                            "/",
                            limit = segments.lastIndex,
                            truncated = ""
                        )
                        RemoteStorage.downloadImages(path, event.eventDrawables)
                    }
                    // It is required to do this with coroutines because when all the drawables
                    // are downloaded the list need to be reattached in order to refresh the
                    // the views
                }.invokeOnCompletion {
                    field.value = list
                }
            }, { })
            return field
        }

    var processState: State = State.IN_PROCESS

    /** Message to be shown when any change has been done */
    val message: MutableLiveData<String> = MutableLiveData()

    /**
     *  Saving an [Event] to the database
     *
     *  @param event The Event to be stored
     *  @param message The message to show to the user if desired
     *  @param onSuccess Actions to do in case the event was successfully stored
     * */
    fun addEvent(event: Event, message: String, onSuccess: (String) -> Unit) {
        val list = events.value ?: mutableListOf()
        list.add(event)
        this.message.value = message
        events.value = list
        processState = State.IN_PROCESS

        viewModelScope.launch {
            RemoteDatabase.createEvent(event.copy(), {
                processState = State.SUCCESS
                onSuccess(it)
            }, {
                processState = State.FAILURE
            })
        }
    }

    /**
     * Request information of an specified event to the database
     *
     * @param eventId The unique identifier of the event
     * @param onSuccess Actions to do with the [Event] requested
     * */
    fun getEventInfo(eventId: String, onSuccess: (Event) -> Unit) {
        RemoteDatabase.getEvent(eventId, onSuccess) { /*Nothing on failure*/ }
    }
}