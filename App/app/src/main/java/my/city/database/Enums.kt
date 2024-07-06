/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.database

enum class RemoteDBCollections(val value: String) {
    COINS("coins"), EVENTS("events"), USERS("users"), CREATED_EVENTS("createdEvents"),
    JOINED_EVENTS("joinedEvents"), LIKED_EVENTS("likedEvents"), PRIVATE("private"),
    CHALLENGES("challenges"), GUESTS("guests")
}

enum class RemoteDBFields(val value: String) {
    NAME("name"), LOCATION("location"), START_EVENT("startEvent"),
    COINS("coins"), EMAIL("email"), ID("id"), BIRTHDATE("birthdate"),
    GENDER("gender")
}

enum class Tags {
    /** An error generated from the remote database. It's critical, the app shouldn't continue*/
    REMOTE_DATABASE,

    /** An error generated when either uploading the photo or downloading it. It's not critical, the app
     * should continue*/
    PROFILE_PHOTO_ERROR,
    PROFILE_DOCUMENT,

    /** An error generated when either uploading the user information or downloading it. The app shouldn't
     * advance and request the user to try again*/
    PROFILE_DOCUMENT_ERROR,

    /** When a severe error at the moment of creating an account occurs where the app shouldn't continue*/
    SIGNING_ERROR,
    EXISTING_EMAIL,

    /** When a severe error occurs where the app should not continue*/
    LOGIN_ERROR,
    LOGIN_FAIL
}


enum class FilterPattern(val value: String) {
    USER_NAME("[a-zA-Z0-9_.]*"), USERS("(@[a-zA-Z0-9_.]*)*"),
    EMAIL("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
    EMAIL_CHARS("[a-zA-Z0-9_.@]*"), PASSWORD_CHARS("[a-zA-Z\\d@$!%*?&)(]*")
}