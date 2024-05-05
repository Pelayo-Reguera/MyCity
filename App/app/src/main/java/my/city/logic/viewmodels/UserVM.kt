/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic.viewmodels

import androidx.lifecycle.ViewModel
import my.city.database.RemoteDatabase
import my.city.logic.User

class UserVM : ViewModel() {

    private val user: User by lazy { User("", "", mutableListOf(), mutableMapOf()) }

    suspend fun getUser(): String {
//        if (user.userName.isBlank()) {
//            result = RemoteDatabase.getProfileInfo().await()["email"].toString()
        val result = RemoteDatabase.getProfileInfo()
//        }

        // If result != null then the block of code inside let is executed. In this block "email" key
        // is accessed and converted its value to a String
        // If result == null then returns an empty String
        return result?.let { return it["email"] as String } ?: ""
    }
}