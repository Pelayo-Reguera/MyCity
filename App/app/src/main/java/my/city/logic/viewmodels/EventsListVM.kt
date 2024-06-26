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
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.launch
import my.city.database.RemoteDatabase
import my.city.database.Tags
import my.city.logic.Event

enum class State {
    IN_PROCESS, SUCCESS, FAILURE
}

class EventsListVM : ViewModel() {

    val events: MutableLiveData<MutableList<Event>> = MutableLiveData(mutableListOf())
        get() {
            RemoteDatabase.getEvents({ it ->
                val list: MutableList<Event> = field.value ?: mutableListOf()
                for (document in it.documents) {
                    // For each document, convert it to an Event and request its images
                    try {
                        document.toObject<Event>()?.let { event ->
                            list.add(event)
                            viewModelScope.launch {
                                event.eventImgURIs.lastOrNull()?.let { uri ->
                                    val segments = Uri.parse(uri).pathSegments
                                    val path = segments.joinToString(
                                        "/",
                                        limit = segments.lastIndex,
                                        truncated = ""
                                    )
                                    RemoteDatabase.downloadImages(path, event.eventDrawables)
                                }
                                // It is required to do this with coroutines because when all the drawables
                                // are downloaded the list need to be updated in order to refresh the
                                // the views
                            }.invokeOnCompletion { field.value = list }
                        }
                    } catch (re: RuntimeException) {
                        Log.e("EventsListVM", "An object is not well constructed in the database")
                    }
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
                Log.e(Tags.REMOTE_DATABASE_ERROR.toString(), it.message.toString())
            })
        }
    }
}