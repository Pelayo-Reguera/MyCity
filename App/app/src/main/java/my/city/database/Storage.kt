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
import android.util.Log
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
import my.city.logic.Event
import java.io.File

/**
 * Singleton object to establish connections with the remote storage of multimedia content
 * */
object RemoteStorage {

    /**
     * Downloads the profile photo of the specified user following the standard URI for profile
     * photos in this app
     *
     * @param userName The original name of the user when its account was created
     * @param onSuccess Actions to do when everything was correct
     * @param onFailure Actions to do when an error arose
     * */
    fun downloadProfilePhoto(
        userName: String,
        onSuccess: (String, File) -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        val storage = Firebase.storage.reference
        val file = File.createTempFile(
            "image",
            "jpg"
        )
        val url = "${RemoteDBCollections.USERS.value}/${userName}/profile.jpg"
        storage.child(url).getFile(file).addOnSuccessListener { onSuccess(url, file) }
            .addOnFailureListener {
                Log.w(
                    Tags.PROFILE_PHOTO_ERROR.toString(),
                    "There was an error downloading the photo",
                    it
                )
                onFailure(Tags.PROFILE_PHOTO_ERROR)
            }
    }

    // INFO: This function would be used in case other sign in methods like Google, Facebook, Apple, etc
    //  were allowed
//    fun downloadPhotoFromURL(
//        url: String,
//        onSuccess: (File) -> Unit,
//        onFailure: (Exception) -> Unit,
//    ) {
//        val storage = Firebase.storage.getReferenceFromUrl(url)
//        val file = File.createTempFile(
//            "image",
//            "jpg"
//        )
//        storage.getFile(file).addOnSuccessListener {
//            onSuccess(file)
//        }.addOnFailureListener {
//            Log.w(
//                Tags.PROFILE_PHOTO_ERROR.toString(),
//                "There was a problem downloading the photo from the URL",
//                it
//            )
//            onFailure(it)
//        }
//    }

    /**
     * It stores an image in the standard path of the profile photo of a user
     *
     * @param file The path of the photo stored in the local device
     * @param userName The original user name when its account was created
     * @param onSuccess Actions to do when everything was correct
     * @param onFailure Actions to do when an error arose
     * */
    fun storeProfilePhoto(
        file: Uri,
        userName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        val storage = Firebase.storage.reference
        val url = "${RemoteDBCollections.USERS.value}/${userName}/profile.jpg"
        storage.child(url).putFile(Uri.parse(file.toString()))
            .addOnSuccessListener { onSuccess(url) }
            .addOnFailureListener { e ->
                Log.w(
                    Tags.PROFILE_PHOTO_ERROR.toString(),
                    "There was a problem uploading the profile photo",
                    e
                )
                onFailure(Tags.PROFILE_PHOTO_ERROR)
            }
    }

    /**
     * Upload the images of the [Event] and update its URIs with the URLs of the remote database
     * instead of the ones from the local device
     *
     * @param id The document where to store the references to images in Firebase Storage
     * @param uris The URIs of the images pointing to the local memory. Later will be the URLs from
     *             the remote database
     * @param onSuccess Actions to do when everything was OK
     * @param onFailure Actions to do when an [Exception] arose
     * */
    suspend fun storeImages(
        id: String,
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
                        "${RemoteDBCollections.EVENTS.value}/${id}/image${counter}.jpg"
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