/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.logic.viewmodels

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import my.city.database.RemoteDatabase
import my.city.logic.User

//TODO: Make the properties of User observable with LiveData
class UserVM : ViewModel() {

    lateinit var user: User
        private set

    private var currentUser: FirebaseUser? = Firebase.auth.currentUser
    lateinit var signInLauncher: ActivityResultLauncher<Intent>

    fun signIn() {
        //INFO: Authentication only when needed, allow use the app in local mode
        if (currentUser == null) {
            // Chosen authentication providers
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.AnonymousBuilder().build(),
            )

            // Create and launch sign-in intent
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
            signInLauncher.launch(signInIntent)
            currentUser = Firebase.auth.currentUser
        }

        setUserData()
    }

    private fun setUserData() {
        currentUser?.let {
            user = User(
                it.displayName.toString(),
                it.email.toString(),
                it.photoUrl.toString(),
                mutableListOf(),
                mutableMapOf(),
                it.isAnonymous,
            )
        }
    }

    fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        //TODO: Add some behaviour to the response
        val response = result.idpResponse
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Successfully signed in
            setUserData()
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
        }
    }

    suspend fun getUser(): String {
//        if (user.userName.isBlank()) {
//            result = RemoteDatabase.getProfileInfo().await()["email"].toString()
        val result: DocumentSnapshot?
        if (currentUser == null) {
            // Chosen authentication providers
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
            )

            // Create and launch sign-in intent
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
            signInLauncher.launch(signInIntent)
        }
        result = RemoteDatabase.getProfileInfo()
//        }

        // If result != null then the block of code inside let is executed. In this block "email" key
        // is accessed and converted its value to a String
        // If result == null then returns an empty String
        //TODO: Return the whole user object
        return result?.let { return it["email"] as String } ?: ""
    }
}