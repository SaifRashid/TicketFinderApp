package com.example.ticketfinderapp

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MyRecyclerAdapter(private val events: ArrayList<Event>) : RecyclerView.Adapter<MyRecyclerAdapter.MyViewHolder>() {
    // Provide a reference to the views for each data item
// Complex data items may need more than one view per item, and
// you provide access to all the views for a data item in a view holder.
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image = itemView.findViewById<ImageView>(R.id.imageView)
        val title = itemView.findViewById<TextView>(R.id.textView_title)
        val location = itemView.findViewById<TextView>(R.id.textView_location)
        val address = itemView.findViewById<TextView>(R.id.textView_address)
        val date = itemView.findViewById<TextView>(R.id.textView_date)
        val range = itemView.findViewById<TextView>(R.id.textView_range)
        val button = itemView.findViewById<Button>(R.id.button_tickets)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
        return MyViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val highestQualityImage = events[position].images.maxByOrNull {
            it.width.toInt() * it.height.toInt()
        }

        val context = holder.itemView.context

        Glide.with(context)
            .load(highestQualityImage?.url)
            .into(holder.image)

        holder.title.text = events[position].name
        holder.location.text = "${events[position]._embedded.venues[0].name}, ${events[position]._embedded.venues[0].city.name}"
        holder.address.text = "${events[position]._embedded.venues[0].address.line1}, ${events[position]._embedded.venues[0].city.name}, ${events[position]._embedded.venues[0].state.stateCode}"

        var dateText = events[position].dates.start.localDate
        var timeText = events[position].dates.start.localTime

        val dateParts = dateText.split("-")
        dateText = "${dateParts[1]}/${dateParts[2]}/${dateParts[0]}"

        val timeParts = timeText.split(":")
        val hour = timeParts[0].toInt()
        val amPm = if (hour < 12) "AM" else "PM"
        timeText = "${if (hour > 12) hour - 12 else hour}:${timeParts[1]} $amPm"

        holder.date.text = "$dateText @ $timeText"

        if (events.getOrNull(position)?.priceRanges?.isNotEmpty() == true) {
            holder.range.text = "Price Range: $${events[position].priceRanges[0].min} - $${events[position].priceRanges[0].max}"
            holder.range.visibility = View.VISIBLE
        } else {
            holder.range.visibility = View.INVISIBLE
        }

        holder.button.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.data = Uri.parse(events[position].url)
            holder.itemView.context.startActivity(browserIntent)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return events.size
    }
}