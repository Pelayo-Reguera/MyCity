package com.example.mycity.logic.dataclasses

/**
 * It represents an User with all the iformation attached to it
 *
 * @property id
 * @property userName
 * @property likedEvents A [MutableList] containing all the events that the user like or is register
 * as an attendant
 * @property coins A [MutableList] containing the quantities of each coin type in the user's possession
 *
 * @author Pelayo Reguera García
 * */
data class User(
    val id: String,
    val userName: String,
    val likedEvents: MutableList<Event>,
    val coins: MutableMap<String, Int>
) {}