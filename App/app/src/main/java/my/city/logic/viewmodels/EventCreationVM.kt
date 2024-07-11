/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic.viewmodels

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.GeoPoint
import my.city.database.RemoteDatabase
import my.city.database.RemoteStorage
import my.city.logic.Challenge
import java.time.LocalDateTime

/**
 * [ViewModel] in charge of storing all the information related to an [Event][my.city.logic.Event]
 * when it is being created. Its content will disappear in case the process is cancelled
 *
 * @author Pelayo Reguera García
 * */
class EventCreationVM : ViewModel() {
    var name: String = ""
    val eventDrawables: MutableList<Drawable> by lazy { mutableListOf() }
    val eventImgURIs: MutableList<String> by lazy { mutableListOf() }
    val organizers: HashMap<String, Drawable?> by lazy { hashMapOf() }
    var description: String = ""
    val challenges: MutableList<Challenge> by lazy { mutableListOf() }
    var location: GeoPoint? = null
    var street: String = ""
    var startEvent: LocalDateTime? = null
    var endEvent: LocalDateTime? = null
    var guestsCapacity: Int = 10

    /** Used for searching possible organizers to add to the Event composed of <UserName, ID>*/
    val usersFound: HashMap<String, String> = hashMapOf()

    /** Determines whether a search is currently being processed or not*/
    var isSearching: Boolean = false

    /** Determines whether there is a pending request or not */
    var requestSearch: Boolean = false

    /**
     *  Finds possible coincidences of users given a username. New users extracted from the database
     *  are stored in [usersFound] for performance reasons
     *
     *  @param userName The username with which the search will be done
     *  @param onSuccess Actions to do with the list of usernames found
     * */
    fun findPossibleUser(userName: String, onSuccess: (List<String>) -> Unit) {
        val result: MutableList<String> = mutableListOf()
        val list: Map<String, String> = usersFound.filter { it.key.matches(Regex(".*$userName.*")) }
        var counter = 0
        if (list.isEmpty()) {
            RemoteDatabase.findUser(userName, {
                usersFound.putAll(it)
                it.forEach { (key, _) ->
                    if (!organizers.containsKey(key) && counter < 3) {
                        result.add(key)
                        counter++
                    }
                }
                onSuccess(result)
            },
                {
                    onSuccess(listOf())
                })
        } else {
            list.forEach { (key, _) ->
                if (!organizers.containsKey(key) && counter < 3) {
                    result.add(key)
                    counter++
                }
            }
            onSuccess(result)
        }
    }

    /**
     * It adds the username in the [organizers] list and tries to download its profile photo and store it
     * in that list. If the profile photo is not available a **``null``** value is stored.
     *
     * @param userName
     * */
    fun addOrganizer(userName: String) {
        RemoteStorage.downloadProfilePhoto(usersFound[userName].toString(),
            { file ->
                Drawable.createFromPath(file.path)?.let { drawable ->
                    organizers[userName] = drawable
                }
            },
            {
                // Do nothing
            }
        )
        organizers[userName] = null
    }
}