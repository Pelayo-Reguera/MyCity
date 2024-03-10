package com.example.mycity.logic.dataclasses

/**
 * It represents all the information a challenge created for a specific event can contain
 *
 * @property name The name of the challenge
 * @property description
 * @property coinID The ID of a specific coin
 * @property reward The amount of coins that this challenge gives to whoever completes it
 *
 * @author Pelayo Reguera García
 * */
data class Challenge(val name: String, val description: String, val coinID: Int, val reward: Int)