/*
 * Copyright (c) 2024.
 * MyCity © 2024 by Pelayo Reguera García is licensed under
 * Attribution-NonCommercial-NoDerivatives 4.0 International.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package my.city.ui.explorer.event

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import my.city.R

/**
 * This class is in charge of generating components based on a layout which later will be
 * used in a recycler view
 *
 * @author Pelayo Reguera García
 * */
class EventImagesAdapter(
    private val eventImagesList: List<Drawable>,
    private val addImage: ActivityResultLauncher<PickVisualMediaRequest>? = null,
) : RecyclerView.Adapter<EventImagesAdapter.ChallengeViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ChallengeViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val imgChallenge: ShapeableImageView = view.findViewById(R.id.imgEvent)

        fun bindEvent(
            position: Int,
            addImage: ActivityResultLauncher<PickVisualMediaRequest>?,
            img: Drawable,
        ) {
            if (position == 0 && addImage != null) {
                imgChallenge.isClickable = true
                imgChallenge.setOnClickListener {
                    addImage.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            } else {
                imgChallenge.load(img)
            }
        }
    }

    // Create new cards (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ChallengeViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.carousel_image, viewGroup, false)

        return ChallengeViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ChallengeViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bindEvent(position, addImage, eventImagesList[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = eventImagesList.size
}