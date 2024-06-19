/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import my.city.logic.Event
import my.city.logic.User

enum class Tags {
    REMOTE_DATABASE_ERROR
}

enum class RemoteDBCollections(val value: String) {
    COINS("coins"), EVENTS("events"), USERS("users"), CREATED_EVENTS("createdEvents"),
    JOINED_EVENTS("joinedEvents"), LIKED_EVENTS("likedEvents"),
    CHALLENGES("challenges"), GUESTS("guests")
}

enum class RemoteDBFields(val value: String) {
    START_EVENT("startEvent")
}

/*
 * TODO: Check if any pattern could be apply here (there is one when you need like different
 *  configurations to do something. In this case, different types of remote and local databases
 */
/**
 * Singleton object for connections purposes with the remote database, in this case Firebase Firestore
 * */
object RemoteDatabase {

    lateinit var user: User

    /** This property is used for pagination purposes*/
    private var lastPos: Int = 0

    /** This property is used for limit the number of results*/
    private var maxNResults: Int = 50

    suspend fun getProfileInfo(): DocumentSnapshot? {
        val db = Firebase.firestore
        //TODO: Change the hardcoded string in the document path
        return db.collection(RemoteDBCollections.USERS.value).document("admin").get().await()
    }

    suspend fun getUserCreatedEvents(): QuerySnapshot? {
        val db = Firebase.firestore
        //TODO: Change the hardcoded string in the document path
        return db.collection(RemoteDBCollections.USERS.value).document("admin")
            .collection(RemoteDBCollections.CREATED_EVENTS.value).get().await()
    }

    suspend fun getUserLikedEvents(): QuerySnapshot? {
        val db = Firebase.firestore
        //TODO: Change the hardcoded string in the document path
        return db.collection(RemoteDBCollections.USERS.value).document("admin")
            .collection(RemoteDBCollections.LIKED_EVENTS.value).get().await()
    }

    suspend fun getUserJoinedEvents(): QuerySnapshot? {
        val db = Firebase.firestore
        //TODO: Change the hardcoded string in the document path
        return db.collection(RemoteDBCollections.USERS.value).document("admin")
            .collection(RemoteDBCollections.JOINED_EVENTS.value).get().await()
    }

    /**
     *  Retrieves at most [maxNResults] of events created so far. In each call, the range of events
     *  changes, for example, the first call could be the first 50 events and the next one from 51 to
     *  100
     *
     *  @param onSuccess Actions to do when the query was ok
     *  @param onFailure Actions to do when an error arose
     * */
    fun getEvents(onSuccess: (QuerySnapshot) -> Unit, onFailure: (Exception) -> Unit) {
        val db = Firebase.firestore
        addGetListenersBehaviours(
            db.collection(RemoteDBCollections.EVENTS.value)
                .orderBy(RemoteDBFields.START_EVENT.value)
                .startAt(lastPos)
                .endBefore(lastPos + maxNResults)
                .get(),
            onSuccess, onFailure,
        )
    }

    fun createEvent(
        event: Event,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val db = Firebase.firestore
        addSetListenersBehaviours(
            db.collection(RemoteDBCollections.EVENTS.value).add(event), onSuccess, onFailure,
        )
    }

    /**
     * Actions to execute when a query of type 'Get' has been executed
     *
     * @param query The query to the remote database itself
     *  @param onSuccess Actions to do when the query was ok
     *  @param onFailure Actions to do when an error arose
     * */
    private fun addGetListenersBehaviours(
        query: Task<QuerySnapshot>,
        onSuccess: (QuerySnapshot) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        query.addOnSuccessListener {
            if (it.size() > 0) {
                lastPos += maxNResults
                onSuccess(it)
            }
        }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    /**
     * Actions to execute when a query of type 'Set' has been executed
     *
     * @param query The query to the remote database itself
     *  @param onSuccess Actions to do when the query was ok
     *  @param onFailure Actions to do when an error arose
     * */
    private fun addSetListenersBehaviours(
        query: Task<DocumentReference>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        query.addOnSuccessListener {
            onSuccess()
        }
            .addOnFailureListener {
                onFailure(it)
            }
    }
}