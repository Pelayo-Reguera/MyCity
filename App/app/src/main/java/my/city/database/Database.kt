/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.database

import android.graphics.drawable.Drawable
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import my.city.logic.Challenge
import my.city.logic.Event
import my.city.logic.User
import java.io.File

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
    private var lastEvent: Event? = null

    /** This property is used for limit the number of results*/
    private var maxNResults: Long = 50

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
        val query = db.collection(RemoteDBCollections.EVENTS.value)
            .orderBy(RemoteDBFields.START_EVENT.value)
            .startAfter(lastEvent?.startEvent).limit(maxNResults)
        addGetListenersBehaviours(query, onSuccess, onFailure)
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

        storeImages(
            doc,
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
     * Actions to execute when a query of type 'Get' has been executed
     *
     * @param query The query to the remote database itself
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
                it.documents.last().toObject<Event>()?.let { it1 -> lastEvent = it1 }
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

    /**
     * Upload the images of the [Event] and update its URIs with the URLs of the remote database
     * instead of the ones from the local device
     *
     * @param doc The document where to store the references to images in Firebase Storage
     * @param uris The URIs of the images pointing to the local memory. Later will be the URLs from
     *             the remote database
     * @param onSuccess Actions to do when everything was OK
     * @param onFailure Actions to do when an [Exception] arose
     * */
    private suspend fun storeImages(
        doc: DocumentReference,
        uris: MutableList<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        coroutineScope {
            val storage = Firebase.storage.reference
            val processes: MutableList<Deferred<String>> = mutableListOf()
            var urls: List<String> = mutableListOf()
            for ((counter, uri) in uris.withIndex()) {
                // Upload each image concurrently
                processes.add(async {
                    val url =
                        "${RemoteDBCollections.EVENTS.value}/${doc.id}/image${counter}.jpg"
                    storage.child(url).putFile(Uri.parse(uri)).await()
                    url
                })
            }
            try {
                urls = processes.awaitAll()
                uris.clear()
                uris.addAll(urls)
                onSuccess()
            } catch (e: Exception) {
                storage.activeUploadTasks.forEach { it.cancel() }
                urls.forEach { url -> storage.child(url).delete() }
                onFailure(e)
            }
        }
    }

    /**
     * It send a request to download each image of the event in different coroutines.
     * This function is not managed with callbacks because the lists containing the events need to
     * be updated after this process and with callbacks is not possible.
     *
     * @param path The path to the folder containing all the images of the [Event]
     * @param eventList The list of the event to fill with its drawables
     * */
    suspend fun downloadImages(path: String, eventList: MutableList<Drawable>) {
        coroutineScope {
            val storage = Firebase.storage.reference
            val list = storage.child(path).listAll().await()
            val jobs: MutableList<Job> = mutableListOf()
            for (item in list.items) {
                jobs.add(launch {
                    val file = File.createTempFile(
                        "image",
                        "jpg"
                    )
                    storage.child(item.path).getFile(file).await()
                    Drawable.createFromPath(file.path)?.let { it1 -> eventList.add(it1) }
                })
            }
            jobs.joinAll()
        }
    }
}