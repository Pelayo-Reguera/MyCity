/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic.viewmodels

import androidx.lifecycle.ViewModel
import my.city.logic.Challenge
import my.city.logic.Location
import my.city.logic.User
import java.time.LocalDateTime

/**
 * [ViewModel] in charge of storing all the information related to an [Event][my.city.logic.Event]
 * when it is being created. Its content will disappear in case the process is cancelled
 *
 * @author Pelayo Reguera García
 * */
class EventCreationVM : ViewModel() {

    lateinit var name: String
    val organizers: MutableList<User> by lazy { mutableListOf() }
    lateinit var description: String
    val challenges: MutableList<Challenge> by lazy { mutableListOf() }
    lateinit var location: Location
    lateinit var startEvent: LocalDateTime
    lateinit var endEvent: LocalDateTime
    val guests: MutableList<User> by lazy { mutableListOf() }
}