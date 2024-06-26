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
import my.city.logic.Challenge
import my.city.logic.User
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
    val organizers: MutableList<User> by lazy { mutableListOf() }
    var description: String = ""
    val challenges: MutableList<Challenge> by lazy { mutableListOf() }
    var location: GeoPoint? = null
    var street: String = ""
    var startEvent: LocalDateTime? = null
    var endEvent: LocalDateTime? = null
    val guests: MutableList<User> by lazy { mutableListOf() }
}