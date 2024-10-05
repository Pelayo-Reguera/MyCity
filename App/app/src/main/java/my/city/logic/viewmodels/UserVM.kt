/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic.viewmodels

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.tomtom.sdk.location.GeoPoint
import my.city.database.RemoteDatabase
import my.city.database.RemoteStorage
import my.city.database.Tags

/**
 * It represents all the information of the user currently using the app. Its purpose is:
 *
 * - To keep the information available in all the application
 * - To be a communication channel between the UI and the [database layer][RemoteDatabase]
 * - Manage
 *
 */
class UserVM : ViewModel() {

    var userName: MutableLiveData<String> = MutableLiveData("")
    var email: String = ""
    var profilePhoto: MutableLiveData<Drawable> = MutableLiveData()
    var location: GeoPoint = GeoPoint(39.56885126840019, -3.301451387486932) //Spain

    /** A list of friends containing only their user names and profile photos */
    var friends: MutableList<Pair<String, Drawable>> = mutableListOf()
    val createdEventsIds: MutableLiveData<MutableList<String>> = MutableLiveData()
        get() {// INFO: Update in every call because friends will be able to include the user as
            //    an organizer of their events (collaborations)
            userName.value
                    .takeIf { !it.isNullOrBlank() && field.value.isNullOrEmpty() && isAnonymous.value == false }
                    ?.let { username ->
                        RemoteDatabase.getUserCreatedEvents(username, {
                            field.value = it.toMutableList()
                        }, {})
            }

            return field
        }
    val likedEventsIds: MutableLiveData<MutableList<String>> = MutableLiveData()
        get() {// It is continuously updated
            userName.value
                    .takeIf { !it.isNullOrBlank() && field.value.isNullOrEmpty() && isAnonymous.value == false }
                    ?.let { username ->
                        RemoteDatabase.getUserLikedEvents(username, {
                            field.value = it.toMutableList()
                        }, {})
                    }

            return field
        }
    val joinedEventsIds: MutableLiveData<MutableList<String>> = MutableLiveData()
        get() {// It is continuously updated
            userName.value
                    .takeIf { !it.isNullOrBlank() && field.value.isNullOrEmpty() && isAnonymous.value == false }
                    ?.let { username ->
                        RemoteDatabase.getUserJoinedEvents(username, {
                            field.value = it.toMutableList()
                        }, {})
                    }

            return field
        }
    val coins: MutableLiveData<MutableMap<String, Int>> = MutableLiveData()
    var birthdate: Timestamp = Timestamp.now()
    var gender: String = ""
    var isAnonymous: MutableLiveData<Boolean> = MutableLiveData(true)

    /** Whether the user is connected to internet or not */
    var isConnected = false
    val currentUser: MutableLiveData<FirebaseUser> = MutableLiveData(Firebase.auth.currentUser)

    /**
     * This function creates a user with the email and password given in the database of authentications.
     *
     * When the user was correctly created, then updates the displayName of the [auth] property with
     * the [userName] to be consistent the information stored in the user information from the authenticator
     * with the user's document stored in the general database. Once this has been done, the token
     * for the authenticated user is requested in order to use it in all later transactions with the
     * database rules.
     *
     * If everything was completely successfully it proceeds to store all the information
     * provided by the user in documents in the general database
     *
     * @param imgProfile The [Uri] to the file stored in the local device
     * @param userName First user name established by the user. It can be edited by the user in the future
     * to show the new one to other users, but the ID of the documents will remain the same and the new created
     * ones will still use the original user name
     * @param email The email given by the user when crating the account
     * @param password
     * @param location The city chosen by the user as the default location when creating the account
     * @param birthDate
     * @param gender
     * @param onSuccess Actions to do when user's document was created successfully
     * @param onFailure Actions to do when an error arose (normally show them in the UI to the user)
     *
     * @see [createUserData]
     * */
    fun signIn(
        imgProfile: Uri?,
        userName: String,
        email: String,
        password: String,
        location: GeoPoint,
        birthDate: Timestamp,
        gender: String,
        onSuccess: () -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // Sign in success, update UI with the signed-in user's information
                // We need to set that the token must be updated continuously otherwise connections with
                // the database don't work
                currentUser.value = it.user
                currentUser.value?.let { user ->
                    user.updateProfile(userProfileChangeRequest {
                        displayName = userName
                    }).addOnCompleteListener { _ ->
                        // Needed to check in the server that the document being created is for the
                        // user trying to created and not other one (impostor)
                        currentUser.value?.getIdToken(true)?.addOnSuccessListener { _ ->
                            createUserData(
                                user,
                                imgProfile,
                                location,
                                birthDate,
                                gender,
                                onSuccess,
                                onFailure
                            )
                        }?.addOnFailureListener { e ->
                            Log.e(
                                Tags.SIGNING_ERROR.toString(),
                                "There was a problem with the access token",
                                e
                            )
                            // If sign in fails, display a message to the user.
                            onFailure(Tags.SIGNING_ERROR)
                        }
                    }.addOnFailureListener { e ->
                        Log.w(
                            Tags.REMOTE_DATABASE_ERROR.toString(),
                            "There was an error when updating the user profile information",
                            e
                        )
                        // If sign in fails, display a message to the user.
                        onFailure(Tags.REMOTE_DATABASE_ERROR)
                    }
                }
            }.addOnFailureListener {
                // If sign in fails, display a message to the user.
                Log.i(
                    Tags.EXISTING_EMAIL_FAILURE.toString(),
                    "The email is already registered for another user",
                    it
                )
                onFailure(Tags.EXISTING_EMAIL_FAILURE)
            }
    }

    /**
     * This function retrieves the basic information of an account stored in the authentications
     * database given an email and a password.
     * When the user was correctly authenticated, it calls [getUserData] for getting all its stored
     * data
     *
     * @param email
     * @param password
     * @param onSuccess Actions to do when the user's information was successfully collected
     * @param onFailure Actions to do in case it was nos possible to complete the request
     */
    fun logIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        Firebase.auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            // Log in success, update UI with the signed-in user's information
            // We need to set that the token must be updated continuously otherwise connections with
            // the database don't work
            currentUser.value = it.user
            Firebase.auth.getAccessToken(true)
                .addOnSuccessListener {
                    getUserData(onSuccess, onFailure)
                }.addOnFailureListener { e ->
                    Log.e(
                        Tags.LOGIN_ERROR.toString(),
                        "There was a problem with the access token",
                        e
                    )
                    onFailure(Tags.LOGIN_ERROR)
                }
        }.addOnFailureListener {
            if (it is FirebaseAuthInvalidCredentialsException) {
                Log.d(Tags.LOGIN_FAILURE.toString(), "The credentials are incorrect")
                onFailure(Tags.LOGIN_FAILURE)
            } else {
                Log.d(Tags.REMOTE_DATABASE_ERROR.toString(), "There's no internet connection")
                onFailure(Tags.REMOTE_DATABASE_ERROR)
            }
        }
    }

    /**
     * It requests to the [database layer][RemoteDatabase] the information stored about the provided
     * user
     *
     * @param onSuccess Actions to do when the user's information was successfully collected
     * @param onFailure Actions to do in case it was nos possible to complete the request
     *
     * @see [RemoteDatabase.getPublicAndPrivateUserInfo]
     * */
    fun getUserData(
        onSuccess: () -> Unit,
        onFailure: (Tags) -> Unit,
    ): Boolean {
        currentUser.value?.let {// Check if there is an opened session
            RemoteDatabase.getPublicAndPrivateUserInfo(// Retrieves the user data
                it.displayName.toString(), { user ->
                    userName.value = user.name
                    email = user.email
                    location = GeoPoint(user.location.latitude, user.location.longitude)
                    coins.value = user.coins
                    gender = user.gender
                    birthdate = user.birthdate
                    isAnonymous.value = false
                    onSuccess()
                    RemoteStorage.downloadProfilePhoto(
                        user.name,
                        { file ->
                            Drawable.createFromPath(file.path)?.let { drawable ->
                                profilePhoto.value = drawable
                            }
                        },
                        onFailure
                    )
                },
                onFailure
            )
            return true
        }
        return false
    }

    /**
     * It requests to the [database layer][RemoteDatabase] to store in documents the user's information
     * from the one provided by the user when signing in and the one actually saved in the [auth] object
     *
     * @param userData User information saved in the authentication database
     * @param imgProfile Path to the image stored in the local device for the user's profile
     * @param location The default location of the user
     * @param birthDate
     * @param gender
     * @param onSuccess Actions to do when all the user's data was saved successfully
     * @param onFailure Actions to do when an error arose (normally show them in the UI to the user)
     *
     * @see [RemoteDatabase.createUserInfo]
     * */
    private fun createUserData(
        userData: FirebaseUser,
        imgProfile: Uri?,
        location: GeoPoint,
        birthDate: Timestamp,
        gender: String,
        onSuccess: () -> Unit,
        onFailure: (Tags) -> Unit,
    ) {
        RemoteDatabase.createUserInfo(
            userData.displayName.toString(),
            userData.email.toString(),
            userData.uid,
            com.google.firebase.firestore.GeoPoint(location.latitude, location.longitude),
            birthDate,
            gender,
            {
                userName.value = userData.displayName.toString()
                email = userData.email.toString()
                this.location = location
                birthdate = birthDate
                this.gender = gender
                isAnonymous.value = false
                onSuccess()
                imgProfile?.let { photo ->
                    RemoteStorage.storeProfilePhoto(
                        photo,
                        userData.displayName.toString(),
                        { url ->
                            Drawable.createFromPath(photo.path)?.let { drawable ->
                                currentUser.value?.updateProfile(userProfileChangeRequest {
                                    photoUri = Uri.parse(url)
                                })
                                profilePhoto.value = drawable
                            }
                        },
                        onFailure
                    )
                }
            }, onFailure
        )
    }


    /**
     * Sing out the user's account
     */
    fun signOut() {
        Firebase.auth.signOut()
        currentUser.value = Firebase.auth.currentUser
    }

    /**
     * Delete the user's account from the authentications database
     */
    fun deleteAccount() {
        // TODO
        //INFO: Improve it by deleting or marking for deletion the user data in documents
        Firebase.auth.currentUser?.delete()
    }
}