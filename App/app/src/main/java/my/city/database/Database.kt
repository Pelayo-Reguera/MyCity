/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.database

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/*TODO: Check if any pattern could be apply here (there is one when you need like different
 * configurations to do something. In this case, different types of remote and local databases
 */
object RemoteDatabase {
    suspend fun getProfileInfo(): DocumentSnapshot? {
        val db = Firebase.firestore
        return db.collection("users").document("admin").get().await()
    }
}