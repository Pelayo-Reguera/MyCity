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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.GeoPoint
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

    /** This property is used for limit the number of results of events*/
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
                            Tags.PROFILE_DOCUMENT_ERROR.toString(),
                            "The user's private document doesn't exists"
                        )
                        onFailure(Tags.PROFILE_DOCUMENT_ERROR)
                    }
                }.addOnFailureListener {
                    Log.w(
                        Tags.PROFILE_DOCUMENT_ERROR.toString(),
                        "The private information of the user couldn't be downloaded",
                        it
                    )
                    onFailure(Tags.PROFILE_DOCUMENT_ERROR)
                }
        }, onFailure)
    }

    /**
     * Collects all the user's public information attached to the name provided
     *
     * @param userName The original username when the account was created
     * @param onSuccess Actions to do when the user's information was successfully collected
     * @param onFailure Actions to do in case it was nos possible to complete the request
     * */
    fun getPublicUserInfo(userName: String, onSuccess: (User) -> Unit, onFailure: (Tags) -> Unit) {
        val db = Firebase.firestore
        //INFO: Try to use the name saved in its own document as a field instead of the document ID
        val doc = db.collection(RemoteDBCollections.USERS.value).document(userName)
        doc.get().addOnSuccessListener { docResult ->
            if (docResult.exists()) {
                try {
                    docResult.toObject<User>()?.let(onSuccess)
                } catch (re: RuntimeException) {
                    Log.e(
                        Tags.PROFILE_DOCUMENT_ERROR.toString(),
                        "An object of type User is not well constructed in the database", re
                    )
                    onFailure(Tags.PROFILE_DOCUMENT_ERROR)
                }
            } else {
                Log.i(
                    Tags.PROFILE_DOCUMENT_ERROR.toString(),
                    "The user's document doesn't exists"
                )
                onFailure(Tags.PROFILE_DOCUMENT_ERROR)
            }
        }.addOnFailureListener {
            Log.e(
                Tags.PROFILE_DOCUMENT_ERROR.toString(),
                "Error when retrieving the user's document", it
            )
            onFailure(Tags.PROFILE_DOCUMENT_ERROR)
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
            docRef.collection(RemoteDBCollections.PRIVATE.value).document(name).set(
                hashMapOf(
                    RemoteDBFields.EMAIL.value to email,
                    RemoteDBFields.ID.value to uid,
                    RemoteDBFields.BIRTHDATE.value to birthDate
                )
            ).addOnSuccessListener { onSuccess() }.addOnFailureListener {
                Log.e(
                    Tags.PROFILE_DOCUMENT_ERROR.toString(),
                    "An error has occurred when creating the private user's document",
                    it
                )
                onFailure(Tags.PROFILE_DOCUMENT_ERROR)
            }
        }.addOnFailureListener {
            Log.e(
                Tags.PROFILE_DOCUMENT_ERROR.toString(),
                "An error has occurred when creating the user's document",
                it
            )
            onFailure(Tags.PROFILE_DOCUMENT_ERROR)
        }
    }

    /**
     * Finds users starting with the user name provided
     *
     * @param userName
     * @param onSuccess Actions to do when a list of maximum 10 results is generated containing the
     * public user name of each person associated with its document's ID (the original user name)
     * @param onFailure Actions to do in case it was nos possible to complete the request
     * */
    fun findUser(
        userName: String,
        onSuccess: (Map<String, String>) -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        val db = Firebase.firestore
        val nextLetter = userName.substring(0, userName.lastIndex) +
                (userName.last() + 1)
        val query = db.collection(RemoteDBCollections.USERS.value)
            .whereGreaterThanOrEqualTo(RemoteDBFields.NAME.value, userName)
            .whereLessThan(RemoteDBFields.NAME.value, nextLetter)
            .limit(10)
        query.get().addOnSuccessListener { result ->
            onSuccess(result.documents.associate {
                it.getString(RemoteDBFields.NAME.value).toString() to it.id
            })
        }.addOnFailureListener {
            Log.e(
                Tags.REMOTE_DATABASE_ERROR.toString(),
                "Error finding the user in the database", it
            )
            onFailure(Tags.REMOTE_DATABASE_ERROR)
        }
    }

    /**
     * It includes the specified event in the user's favourite liked events
     *
     * @param eventId The unique identifier of the Event
     * @param userName The original username when the account was created
     * @param onSuccess Actions to do when the user was subscribed correctly
     * @param onFailure Actions to do when it was not possible to subscribe to the Event
     * */
    fun likeEvent(
        eventId: String,
        userName: String,
        onSuccess: () -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        val db = Firebase.firestore
        db.collection(RemoteDBCollections.USERS.value).document(userName)
            .collection(RemoteDBCollections.LIKED_EVENTS.value).document(eventId)
            .set(hashMapOf<String, String>())
            .addOnSuccessListener { onSuccess() }.addOnFailureListener {
                Log.w(
                    Tags.REMOTE_DATABASE_ERROR.toString(),
                    "There was a problem adding to favourites the Event",
                    it
                )
                onFailure(Tags.REMOTE_DATABASE_ERROR)
            }
    }

    /**
     * It removes the specified user from an Event
     *
     * @param eventId The unique identifier of the Event
     * @param userName The original username when the account was created
     * @param onSuccess Actions to do when the user was unsubscribed correctly
     * @param onFailure Actions to do when it was not possible to unsubscribe from the Event
     * */
    fun dislikeEvent(
        eventId: String,
        userName: String,
        onSuccess: () -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        val db = Firebase.firestore
        db.collection(RemoteDBCollections.USERS.value).document(userName)
            .collection(RemoteDBCollections.LIKED_EVENTS.value).document(eventId).delete()
            .addOnSuccessListener { onSuccess() }.addOnFailureListener {
                Log.w(
                    Tags.REMOTE_DATABASE_ERROR.toString(),
                    "There was a problem adding to favourites the Event",
                    it
                )
                onFailure(Tags.REMOTE_DATABASE_ERROR)
            }
    }

    /**
     * It includes the specified user to an Event
     *
     * @param eventId The unique identifier of the Event
     * @param userName The original username when the account was created
     * @param onSuccess Actions to do when the user was subscribed correctly
     * @param onFailure Actions to do when it was not possible to subscribe to the Event
     * */
    fun joinEvent(
        eventId: String,
        userName: String,
        onSuccess: () -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        val db = Firebase.firestore
        val doc = db.collection(RemoteDBCollections.EVENTS.value).document(eventId)

        doc.collection(RemoteDBCollections.GUESTS.value).count().get(AggregateSource.SERVER)
            .addOnSuccessListener { query ->
                db.runTransaction { transaction ->
                    if (query.count < transaction.get(doc)[RemoteDBFields.GUESTS_CAPACITY.value] as Long) {
                        transaction.set(
                            doc.collection(RemoteDBCollections.GUESTS.value).document(userName),
                            hashMapOf<String, String>()
                        )
                        transaction.set(
                            db.collection(RemoteDBCollections.USERS.value).document(userName)
                                .collection(RemoteDBCollections.JOINED_EVENTS.value)
                                .document(eventId),
                            hashMapOf<String, String>()
                        )
                    }
                }.addOnSuccessListener { onSuccess() }.addOnFailureListener {
                    Log.w(
                        Tags.REMOTE_DATABASE_ERROR.toString(),
                        "There was a problem subscribing the user to the Event",
                        it
                    )
                    onFailure(Tags.REMOTE_DATABASE_ERROR)
                }
            }.addOnFailureListener {
                Log.w(
                    Tags.REMOTE_DATABASE_ERROR.toString(),
                    "There was a problem subscribing the user to the Event",
                    it
                )
                onFailure(Tags.REMOTE_DATABASE_ERROR)
            }
    }

    /**
     * It removes the specified user from an Event
     *
     * @param eventId The unique identifier of the Event
     * @param userName The original username when the account was created
     * @param onSuccess Actions to do when the user was unsubscribed correctly
     * @param onFailure Actions to do when it was not possible to unsubscribe from the Event
     * */
    fun disjoinEvent(
        eventId: String,
        userName: String,
        onSuccess: () -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        val db = Firebase.firestore

        db.runBatch {
            it.delete(
                db.collection(RemoteDBCollections.EVENTS.value).document(eventId)
                    .collection(RemoteDBCollections.GUESTS.value).document(userName)
            )

            it.delete(
                db.collection(RemoteDBCollections.USERS.value).document(userName)
                    .collection(RemoteDBCollections.JOINED_EVENTS.value).document(eventId)
            )
        }.addOnSuccessListener { onSuccess() }.addOnFailureListener {
            Log.w(
                Tags.REMOTE_DATABASE_ERROR.toString(),
                "There was a problem removing the user from the Event",
                it
            )
            onFailure(Tags.REMOTE_DATABASE_ERROR)
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
        db.collection(RemoteDBCollections.EVENTS.value)
            .orderBy(RemoteDBFields.START_EVENT.value)
            .startAfter(lastEvent?.startEvent).limit(maxNResults).get().addOnSuccessListener {
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
                            Tags.REMOTE_DATABASE_ERROR.toString(),
                            "An object of type Event is not well constructed in the database"
                        )
                    }
                }
                previousList.value = list

                // Update the reference to the last Event downloaded
                lastEvent = list.last()
            }.addOnFailureListener(onFailure)
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
        db.collection(RemoteDBCollections.EVENTS.value).document(event.id)
            .collection(RemoteDBCollections.CHALLENGES.value).get().addOnSuccessListener {
                for (document in it.documents) {
                    try {
                        document.toObject<Challenge>()?.let { challenge ->
                            event.challenges.add(challenge)
                        }
                    } catch (re: RuntimeException) {
                        Log.e(
                            Tags.CHALLENGE_DOCUMENT_ERROR.toString(),
                            "A Challenge is not well constructed in the database",
                            re
                        )
                    }
                }
            }.addOnFailureListener { onFailure(it) }
    }

    /**
     * It determines whether the user joined the event or not
     *
     * @param userName The original username when the account was created
     * @param eventID The unique identifier of the Event
     * @param onSuccess Actions to do when the user is actually joined the event
     * @param onFailure Actions to do when the user didn't joined to the event
     * */
    fun isUserJoined(
        userName: String,
        eventID: String,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        val db = Firebase.firestore
        db.collection(RemoteDBCollections.EVENTS.value).document(eventID)
            .collection(RemoteDBCollections.GUESTS.value).document(userName).get()
            .addOnSuccessListener { onSuccess(it.exists()) }
            .addOnFailureListener { onFailure(Tags.REMOTE_DATABASE_ERROR) }
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
        onFailure: (Tags) -> Unit,
    ) {
        val db = Firebase.firestore
        val challenges: List<Challenge> = event.challenges.toList()
        val doc = db.collection(RemoteDBCollections.EVENTS.value).document()

        RemoteStorage.storeEventImages(
            doc.id,
            event.eventImgURIs,
            {// When all the images where uploaded correctly
                db.runBatch {
                    it.set(doc, event)
                    for (challenge in challenges) {
                        it.set(
                            doc.collection(RemoteDBCollections.CHALLENGES.value)
                                .document(),
                            challenge
                        )
                    }
                }.addOnSuccessListener { onSuccess() }.addOnFailureListener {
                    Log.e(
                        Tags.REMOTE_DATABASE_ERROR.toString(),
                        "There was a problem creating the event",
                        it
                    )
                    onFailure(Tags.REMOTE_DATABASE_ERROR)
                }
            },
            { onFailure(Tags.REMOTE_DATABASE_ERROR) }
        )
    }
}