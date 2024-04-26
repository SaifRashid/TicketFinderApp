package com.example.ticketfinderapp.ui.favorited

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ticketfinderapp.Event
import com.example.ticketfinderapp.MainActivity
import com.example.ticketfinderapp.MyRecyclerAdapter
import com.example.ticketfinderapp.R
import com.example.ticketfinderapp.TicketMasterData
import com.example.ticketfinderapp.TicketMasterService
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FavoritedFragment : Fragment() {
    private val BASE_URL = "https://app.ticketmaster.com/discovery/v2/"
    private val apikey = "Ayl9reEX22B94IUU01mLxM89kAE1H6ia"
    private val sort = "date,asc"
    private val TAG = "Favorite Fragment"

    private lateinit var loginButton: Button
    private lateinit var favoriteEvents: ArrayList<String>

    private val events = ArrayList<Event>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyRecyclerAdapter

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
        adapter = MyRecyclerAdapter(events, db, favoriteEvents)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        noFavorites = view.findViewById(R.id.textView_no_favorites)

        // If currentUser is not null, we have a user and go back to the MainActivity
        if (user != null) {
            loginButton.visibility = View.GONE
        } else {
            // create a new ActivityResultLauncher to launch the sign-in activity and handle the result
            // When the result is returned, the result parameter will contain the data and resultCode (e.g., OK, Cancelled etc.).
            val signActivityLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        // The user has successfully signed in or he/she is a new user

                        val user = FirebaseAuth.getInstance().currentUser
                        Log.d(TAG, "onActivityResult: $user")

                        //Checking for User (New/Old) (optional--you do not have to show these toast messages)
                        if (user?.metadata?.creationTimestamp == user?.metadata?.lastSignInTimestamp) {
                            //This is a New User
                            val docData = hashMapOf(
                                "favoriteEvents" to arrayListOf<String>()
                            )
                            db.collection("favorites").document(user!!.uid).set(docData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "DocumentSnapshot successfully written!")
                                    Toast.makeText(view.context, "Welcome New User!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error writing document", e)
                                }
                        } else {
                            //This is a returning user
                            Toast.makeText(view.context, "Welcome Back!", Toast.LENGTH_SHORT).show()
                        }

                        // Since the user signed in, the user can go back to main activity
                        startActivity(Intent(view.context, MainActivity::class.java))
                        // Make sure to call finish(), otherwise the user would be able to go back to the RegisterActivity
                        requireActivity().finish()

                    } else {
                        // Sign in failed. If response is null the user canceled the
                        // sign-in flow using the back button. Otherwise check
                        // response.getError().getErrorCode() and handle the error.
                        val response = IdpResponse.fromResultIntent(result.data)
                        if (response == null) {
                            Log.d(
                                TAG,
                                "onActivityResult: the user has cancelled the sign in request"
                            )
                        } else {
                            Log.e(TAG, "onActivityResult: ${response.error?.errorCode}")
                        }
                    }
                }

            // Login Button
            view.findViewById<Button>(R.id.login_button).setOnClickListener {
                // Choose authentication providers -- make sure enable them on your firebase account first
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )

                // Create  sign-in intent
                val signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setTosAndPrivacyPolicyUrls("https://example.com", "https://example.com")
                    .setLogo(R.drawable.baseline_music_video_24)
                    .setIsSmartLockEnabled(false)
                    .build()

                // Launch sign-in Activity with the sign-in intent above
                signActivityLauncher.launch(signInIntent)
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