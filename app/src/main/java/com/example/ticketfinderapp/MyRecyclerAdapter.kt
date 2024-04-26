package com.example.ticketfinderapp

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyRecyclerAdapter(private val events: ArrayList<Event>, private val db: FirebaseFirestore, var favoriteEvents: ArrayList<String>) : RecyclerView.Adapter<MyRecyclerAdapter.MyViewHolder>() {
    // Provide a reference to the views for each data item
// Complex data items may need more than one view per item, and
// you provide access to all the views for a data item in a view holder.
    class MyViewHolder(itemView: View, private val events: ArrayList<Event>, private val db: FirebaseFirestore) : RecyclerView.ViewHolder(itemView) {
        val image = itemView.findViewById<ImageView>(R.id.imageView)
        val title = itemView.findViewById<TextView>(R.id.textView_title)
        val location = itemView.findViewById<TextView>(R.id.textView_location)
        val address = itemView.findViewById<TextView>(R.id.textView_address)
        val date = itemView.findViewById<TextView>(R.id.textView_date)
        val range = itemView.findViewById<TextView>(R.id.textView_range)
        val button = itemView.findViewById<Button>(R.id.button_tickets)
        val favoriteImage = itemView.findViewById<ImageView>(R.id.image_favorite)

        init {
            button.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    events[position].url?.let { url ->
                        val browserIntent = Intent(Intent.ACTION_VIEW)
                        browserIntent.data = Uri.parse(url)
                        itemView.context.startActivity(browserIntent)
                    }
                }
            }
            favoriteImage.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val eventId = events[position].id

                    val user = FirebaseAuth.getInstance().currentUser

                    if (user != null) {
                        val favoritesCollection = db.collection("favorites").document(user.uid)

                        // Update the user's favorite events
                        favoritesCollection.get().addOnSuccessListener { documentSnapshot ->
                            val favoriteEvents = documentSnapshot.get("favoriteEvents") as? ArrayList<String> ?: arrayListOf()

                            if (favoriteEvents.contains(eventId)) {
                                // If the event is already favorited, remove it
                                favoriteEvents.remove(eventId)
                            } else {
                                // If the event is not favorited, add it
                                favoriteEvents.add(eventId)
                            }

                            // Update the "favoriteEvents" field in the user's document
                            favoritesCollection.update("favoriteEvents", favoriteEvents)
                                .addOnSuccessListener {
                                    if (favoriteEvents.contains(eventId)) {
                                        favoriteImage.setImageResource(R.drawable.star)
                                        Toast.makeText(itemView.context, "Event added to favorites", Toast.LENGTH_SHORT).show()
                                    } else {
                                        favoriteImage.setImageResource(R.drawable.no_star)
                                        Toast.makeText(itemView.context, "Event removed from favorites", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(itemView.context, "Failed to update favorites", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // User is not signed in
                        Toast.makeText(
                            itemView.context, "Please sign in to add events to favorites", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
        return MyViewHolder(view, events, db)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val highestQualityImage = events[position].images.maxByOrNull {
            it.width.toInt() * it.height.toInt()
        }

        val context = holder.itemView.context

        if (highestQualityImage != null) {
            Glide.with(context)
                .load(highestQualityImage.url)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.no_image)
        }

        holder.title.text = events[position].name ?: "N/A"

        val venue = events[position]._embedded.venues.getOrNull(0)
        val venueName = venue?.name ?: " VENUE N/A"
        val cityName = venue?.city?.name ?: "CITY N/A"
        holder.location.text = "$venueName, $cityName"
        val addressLine1 = venue?.address?.line1 ?: "ADDRESS N/A"
        val stateCode = venue?.state?.stateCode ?: venue?.country?.countryCode ?: "STATE N/A"
        holder.address.text = "$addressLine1, $cityName, $stateCode"

        var dateText = events[position].dates.start.localDate ?: ""
        var timeText = events[position].dates.start.localTime ?: ""

        dateText = if (dateText.isNotEmpty()) {
            val dateParts = dateText.split("-")
            "${dateParts.getOrNull(1)}/${dateParts.getOrNull(2)}/${dateParts.getOrNull(0) ?: "N/A"}"
        } else
            "DATE N/A"

        timeText = if (timeText.isNotEmpty()) {
            val timeParts = timeText.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
            val amPm = if (hour < 12) "AM" else "PM"
            "${if (hour > 12) hour - 12 else hour}:${timeParts.getOrNull(1) ?: "N/A"} $amPm"
        } else
            "TIME N/A"

        holder.date.text = "$dateText @ $timeText"

        if (events[position].priceRanges?.isNotEmpty() == true) {
            holder.range.text = "Price Range: $${events[position].priceRanges[0].min} - $${events[position].priceRanges[0].max}"
            holder.range.visibility = View.VISIBLE
        } else {
            holder.range.visibility = View.INVISIBLE
        }

        if (favoriteEvents.contains(events[position].id)) {
            holder.favoriteImage.setImageResource(R.drawable.star)
        } else {
            holder.favoriteImage.setImageResource(R.drawable.no_star)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return events.size
    }
}