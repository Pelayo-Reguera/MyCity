/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.database

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import my.city.logic.Challenge
import my.city.logic.Event
import my.city.logic.User

/*
 * TODO: Check if any pattern could be apply here (there is one when you need like different
 *  configurations to do something. In this case, different types of remote and local databases
 */
/**
 * Singleton object for connections purposes with the remote database, in this case Firebase Firestore
 * */
object RemoteDatabase {

    /** This property is used for pagination purposes*/
    private var lastEvent: Event? = null

    /** This property is used for limit the number of results*/
    private var maxNResults: Long = 50

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
     *  It requests all the information attached to a user specified by its name both public and private
     *  <b> except the collections related to it like the events joined, friends, etc </b>
     *
     *  @param userName The one when the account was created
     *  @param onSuccess Actions to do when the user's information was successfully collected
     *  @param onFailure Actions to do in case it was nos possible to complete the request
     * */
    fun getPublicAndPrivateUserInfo(
        userName: String,
        onSuccess: (User) -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        // The getPublicUserInfo function is called and in case it was successful a
        // request to the private information about the user is made. Similar to the Wrapper Pattern
        getPublicUserInfo(userName, { user ->
            val db = Firebase.firestore
            val doc = db.collection(RemoteDBCollections.USERS.value).document(userName)
            doc.collection(RemoteDBCollections.PRIVATE.value).document(userName).get()
                .addOnSuccessListener { result ->
                    //INFO: Check if the outer try-catch controls an exception in this
                    // conversion
                    val privateInfo: User? = result.toObject<User>()
                    if (privateInfo != null) {
                        privateInfo.name = user.name
                        privateInfo.location = user.location
                        privateInfo.gender = user.gender
                        onSuccess(privateInfo)
                    } else {
                        Log.i(
                            Tags.PROFILE_DOCUMENT.toString(),
                            "The user's private document doesn't exists"
                        )
                        onFailure(Tags.PROFILE_DOCUMENT_ERROR)
                    }
                }.addOnFailureListener {
                    Log.w(
                        Tags.PROFILE_DOCUMENT.toString(),
                        "The private information of the user couldn't be downloaded",
                        it
                    )
                    onFailure(Tags.LOGIN_ERROR)
                }
        }, onFailure)
    }

    /**
     * Collects all the user's public information attached to the name provided
     *
     * @param userName
     * @param onSuccess Actions to do when the user's information was successfully collected
     * @param onFailure Actions to do in case it was nos possible to complete the request
     * */
    fun getPublicUserInfo(userName: String, onSuccess: (User) -> Unit, onFailure: (Tags) -> Unit) {
        val db = Firebase.firestore
        //INFO: Try to use the name saved in its own document as a field instead of the document ID
        val doc = db.collection(RemoteDBCollections.USERS.value).document(userName)
        Log.d(
            "SIGNIN",
            "Creando solicitud del documento del usuario $doc"
        )
        doc.get().addOnSuccessListener { docResult ->
            Log.i(
                "SIGNIN",
                "Documento de usuario solicitado con id ${docResult.id} y data ${docResult.data}"
            )
            try {
                val user: User? = docResult.toObject<User>()
                if (user != null) {
                    Log.i(
                        "SIGNIN",
                        "Usuario creado parcialmente $user"
                    )
                    onSuccess(user)
                } else {
                    Log.i(
                        Tags.PROFILE_DOCUMENT.toString(),
                        "The user's document doesn't exists"
                    )
                    onFailure(Tags.PROFILE_DOCUMENT_ERROR)
                }
                Log.d(
                    "SIGNIN",
                    "Proceso Usuario finalizado"
                )
            } catch (re: RuntimeException) {
                Log.e(
                    Tags.REMOTE_DATABASE.toString(),
                    "An object of type User is not well constructed in the database", re
                )
                onFailure(Tags.PROFILE_DOCUMENT_ERROR)
            }
        }.addOnFailureListener {
            Log.e(
                "SIGNIN",
                "Error when retrieving the user's document", it
            )
            onFailure(Tags.LOGIN_ERROR)
        }
    }

    /**
     * Creates the document associated with the user and its private information in the remote database
     *
     * @param name The original user name when its account was created
     * @param email The user's email
     * @param uid The uid of the user for the Firebase present in the token
     * @param location Default location of the user
     * @param birthDate
     * @param gender
     * @param onSuccess Actions to do when user's documents were created successfully
     * @param onFailure Actions to do when an error arose (normally show them in the UI to the user)
     * */
    fun createUserInfo(
        name: String,
        email: String,
        uid: String,
        location: GeoPoint,
        birthDate: Timestamp,
        gender: String,
        onSuccess: () -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        val db = Firebase.firestore
        val docRef = db.collection(RemoteDBCollections.USERS.value).document(name)
        docRef.set(
            hashMapOf(
                RemoteDBFields.NAME.value to name,
                RemoteDBFields.LOCATION.value to location,
                RemoteDBFields.GENDER.value to gender
            )
        ).addOnSuccessListener {
            Log.d(
                "SIGNIN",
                "Éxito en la creacion del documento del user"
            )
            docRef.collection(RemoteDBCollections.PRIVATE.value).document(name).set(
                hashMapOf(
                    RemoteDBFields.EMAIL.value to email,
                    RemoteDBFields.ID.value to uid,
                    RemoteDBFields.BIRTHDATE.value to birthDate
                )
            ).addOnSuccessListener { onSuccess() }.addOnFailureListener {
                Log.e(
                    Tags.REMOTE_DATABASE.toString(),
                    "An error has occurred when creating the private user's document",
                    it
                )
                onFailure(Tags.REMOTE_DATABASE)
            }
        }.addOnFailureListener {
            Log.e(
                Tags.REMOTE_DATABASE.toString(),
                "An error has occurred when creating the user's document",
                it
            )
            onFailure(Tags.REMOTE_DATABASE)
        }
    }

    /**
     *  Retrieves at most [maxNResults] of events created so far. In each call, the range of events
     *  changes, for example, the first call could be the first 50 events and the next one from 51 to
     *  100
     *
     *  @param previousList The list whose value will be updated
     *  @param onSuccess Actions to do when the query was ok
     *  @param onFailure Actions to do when an error arose
     * */
    fun getEvents(
        previousList: MutableLiveData<MutableList<Event>>,
        onSuccess: (MutableList<Event>, Event) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val db = Firebase.firestore
        val query = db.collection(RemoteDBCollections.EVENTS.value)
            .orderBy(RemoteDBFields.START_EVENT.value)
            .startAfter(lastEvent?.startEvent).limit(maxNResults)

        addGetListenersBehaviours(query, {
            val list: MutableList<Event> = previousList.value ?: mutableListOf()
            for (document in it.documents) {
                // For each document, convert it to an Event and request its challenges and execute onSuccess
                try {
                    document.toObject<Event>()?.let { event ->
                        event.id = document.id
                        list.add(event)
                        getChallenges(event, onFailure)
                        onSuccess(list, event)
                    }
                } catch (re: RuntimeException) {
                    Log.e(
                        Tags.REMOTE_DATABASE.toString(),
                        "An object of type Event is not well constructed in the database"
                    )
                }
            }
            previousList.value = list

            // Update the reference to the last Event downloaded
            lastEvent = list.last()
        }, onFailure)
    }

    /**
     * It requests all challenges belonging to the specified [Event]
     *
     * @param event The event to which the requested challenges will be stored
     * @param onFailure Actions to execute in case there is an error when creating the [Challenge] object
     * */
    private fun getChallenges(
        event: Event,
        onFailure: (Exception) -> Unit,
    ) {
        val db = Firebase.firestore
        val query = db.collection(RemoteDBCollections.EVENTS.value).document(event.id)
            .collection(RemoteDBCollections.CHALLENGES.value)
        addGetListenersBehaviours(query, {
            for (document in it.documents) {
                try {
                    document.toObject<Challenge>()?.let { challenge ->
                        event.challenges.add(challenge)
                    }
                } catch (re: RuntimeException) {
                    Log.e(
                        Tags.REMOTE_DATABASE.toString(),
                        "An object of type Challenge is not well constructed in the database"
                    )
                }
            }
        },
            { onFailure(it) })
    }

    /**
     * Actions to execute when a query of type 'Get' has been executed
     *
     *  @param query The query to the remote database itself
     *  @param onSuccess Actions to do when the query was ok
     *  @param onFailure Actions to do when an error arose
     * */
    private fun addGetListenersBehaviours(
        query: Query,
        onSuccess: (QuerySnapshot) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        query.get().addOnSuccessListener {
            if (it.size() > 0) {
                onSuccess(it)
            }
        }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    /**
     * Creates an [Event] in the database
     *
     *  @param onSuccess Actions to do when the query was ok
     *  @param onFailure Actions to do when an error arose
     * */
    suspend fun createEvent(
        event: Event,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val db = Firebase.firestore
        val challenges: List<Challenge> = event.challenges.toList()
        val doc = db.collection(RemoteDBCollections.EVENTS.value).document()

        RemoteStorage.storeImages(
            doc.id,
            event.eventImgURIs,
            {// When all the images where uploaded correctly
                addSetListenersBehaviours(
                    doc.set(event),
                    { // A subcollection named 'challenges' is created in the Event in case it has challenges
                        db.runBatch { it1 ->
                            for (challenge in challenges) {
                                it1.set(
                                    doc.collection(RemoteDBCollections.CHALLENGES.value)
                                        .document(),
                                    challenge
                                )
                            }
                        }.addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it) }
                    },
                    onFailure,
                )
            },
            onFailure
        )
    }

    /**
     * Actions to execute when a query of type 'Set' has been executed
     *
     * @param query The query to the remote database itself
     *  @param onSuccess Actions to do when the query was ok
     *  @param onFailure Actions to do when an error arose
     * */
    private fun addSetListenersBehaviours(
        query: Task<Void>,
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