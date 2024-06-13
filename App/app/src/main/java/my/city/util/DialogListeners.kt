/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.util

import androidx.fragment.app.DialogFragment

/**
 * Interface to be implemented from those classes that somehow call a dialog fragment. Its purpose
 * is to give a behaviour to the main buttons of the [DialogFragment] so that it can communicate
 * somehow with the parent view which calls the dialog
 *
 * @author Pelayo Reguera García
 * */
interface DialogListeners {

    /**
     * Actions to execute when the 'Accept' button is pressed in the dialog
     *
     * @param dialog The rendered dialog which the button 'Accept' belongs to
     * */
    fun onClickAccept(dialog: DialogFragment) {

    }

    /**
     * Actions to execute when the 'Cancel' button is pressed in the dialog
     *
     * @param dialog The rendered dialog which the button 'Cancel' belongs to
     * */
    fun onClickCancel(dialog: DialogFragment) {

    }
}