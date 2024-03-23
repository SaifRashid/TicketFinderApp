package com.example.ticketfinderapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class MyRecyclerAdapter() : RecyclerView.Adapter<MyRecyclerAdapter.MyViewHolder>() {
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
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
        return MyViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.image.setImageResource(R.drawable.baseline_music_video_24)
        holder.title.text = "Hartford Symphony Orchestra"
        holder.location.text = "Bushnell Theatre"
        holder.address.text = "Address: 166 Capitol Ave, Hartford, Connecticut"
        holder.date.text = "Date: 03/23/2024 @ 7:30 PM"
        holder.range.text = "Price Range: \$46.5 - \$86.5"
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return 20
    }
}