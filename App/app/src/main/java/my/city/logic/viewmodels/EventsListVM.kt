/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.city.database.RemoteDatabase
import my.city.database.RemoteStorage
import my.city.database.Tags
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
                viewModelScope.launch {
                    event.eventImgURIs.lastOrNull()?.let { uri ->
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
            }, {
                Log.e(Tags.REMOTE_DATABASE.toString(), it.message.toString())
            })
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
    fun addEvent(event: Event, message: String, onSuccess: () -> Unit) {
        val list = events.value ?: mutableListOf()
        list.add(event)
        this.message.value = message
        events.value = list
        processState = State.IN_PROCESS

        viewModelScope.launch {
            RemoteDatabase.createEvent(event.copy(), {
                processState = State.SUCCESS
                onSuccess()
            }, {
                processState = State.FAILURE
                Log.e(Tags.REMOTE_DATABASE.toString(), it.message.toString())
            })
        }
    }
}