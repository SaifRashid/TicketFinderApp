package com.example.ticketfinderapp.ui.favorited

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ticketfinderapp.BaseFragment
import com.example.ticketfinderapp.Event
import com.example.ticketfinderapp.FavoritedRecyclerAdapter
import com.example.ticketfinderapp.R
import com.example.ticketfinderapp.TicketMasterData
import com.example.ticketfinderapp.TicketMasterService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FavoritedFragment : BaseFragment() {
    private val BASE_URL = "https://app.ticketmaster.com/discovery/v2/"
    private val apikey = "Ayl9reEX22B94IUU01mLxM89kAE1H6ia"
    private val sort = "date,asc"
    private val TAG = "Favorite Fragment"

    private lateinit var loginButton: Button
    private lateinit var favoriteEvents: ArrayList<String>

    private val events = ArrayList<Event>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoritedRecyclerAdapter

    private lateinit var noFavorites: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_favorited, container, false)
        // Write your code
        // Get a Cloud Firestore instance
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        loginButton = view.findViewById(R.id.login_button)

        favoriteEvents = ArrayList()

        recyclerView = view.findViewById(R.id.RecyclerView)
        adapter = FavoritedRecyclerAdapter(events, db, favoriteEvents)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        noFavorites = view.findViewById(R.id.textView_no_favorites)

        login(user, loginButton, db, TAG)
        if (user != null) {
            loginButton.visibility = View.GONE
            db.collection("favorites").document(user.uid).get().addOnSuccessListener { documentSnapshot ->
                favoriteEvents = documentSnapshot.get("favoriteEvents") as? ArrayList<String> ?: arrayListOf()
                adapter.favoriteEvents = favoriteEvents
                adapter.notifyDataSetChanged()
                Log.d(TAG, "Fetched favorite events")
                if (favoriteEvents.isNotEmpty()) {
                    val retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val ticketMasterAPI = retrofit.create(TicketMasterService::class.java)
                    val call: Call<TicketMasterData> =
                        ticketMasterAPI.getFavorites(apikey, sort, favoriteEvents)

                    call.enqueue(object : Callback<TicketMasterData> {
                        override fun onResponse(
                            call: Call<TicketMasterData>,
                            response: Response<TicketMasterData>
                        ) {
                            Log.d(TAG, "OnResponse: $response")
                            val body = response.body()
                            if (body == null) {
                                Log.d(TAG, "Valid response was not received")
                                return
                            }

                            // Check and return if no events
                            if (body._embedded == null || body._embedded.events.isEmpty()) {
                                return
                            }

                            events.clear()
                            events.addAll(body._embedded.events)

                            adapter.notifyDataSetChanged()
                        }

                        override fun onFailure(call: Call<TicketMasterData>, t: Throwable) {
                            Log.d(TAG, "OnFailure: $t")
                        }
                    })
                }
            }
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            db.collection("favorites").document(user.uid).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error fetching favorite events", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    favoriteEvents = snapshot.get("favoriteEvents") as? ArrayList<String> ?: arrayListOf()
                    adapter.favoriteEvents = favoriteEvents
                    adapter.notifyDataSetChanged()
                    Log.d(TAG, "Fetched favorite events")
                } else {
                    Log.d(TAG, "Favorite events document not found")
                }
                updateNoFavorites(if (favoriteEvents.isEmpty()) "No Favorites" else "gone")
            }
        } else {
            updateNoFavorites("Sign in")
        }
    }

    private fun updateNoFavorites(string: String) {
        if (string == "Sign in") {
            noFavorites.text = "Sign in to save your favorite events!"
            noFavorites.visibility = View.VISIBLE
            recyclerView.visibility = View.INVISIBLE
        } else if (string == "No Favorites") {
            noFavorites.text = "No Favorites to display.\nClick the star icon to save your favorite events!"
            noFavorites.visibility = View.VISIBLE
            recyclerView.visibility = View.INVISIBLE
        } else if (string == "gone"){
            noFavorites.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}