package com.example.ticketfinderapp.ui.home

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ticketfinderapp.BaseFragment
import com.example.ticketfinderapp.Event
import com.example.ticketfinderapp.MyRecyclerAdapter
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

class HomeFragment : BaseFragment() {
    private val BASE_URL = "https://app.ticketmaster.com/discovery/v2/"
    private val apikey = "Ayl9reEX22B94IUU01mLxM89kAE1H6ia"
    private val sort = "date,asc"
    private val TAG = "Home Fragment"

    private lateinit var loginButton: Button
    private lateinit var favoriteEvents: ArrayList<String>

    private lateinit var db: FirebaseFirestore

    private val events = ArrayList<Event>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyRecyclerAdapter
    private lateinit var editTextKeyword: EditText
    private lateinit var editTextCity: EditText
    private lateinit var noResults: TextView

    private lateinit var spinner: Spinner
    private lateinit var venueMap: HashMap<String, String>
    private lateinit var venueId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        // Write your code
        // Get a Cloud Firestore instance
        db = FirebaseFirestore.getInstance()
        loginButton = view.findViewById(R.id.button_login)
        val user = FirebaseAuth.getInstance().currentUser
        favoriteEvents = ArrayList()

        recyclerView = view.findViewById(R.id.RecyclerView)
        adapter = MyRecyclerAdapter(events, db, favoriteEvents)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        editTextKeyword = view.findViewById(R.id.editText_keyword)
        editTextCity = view.findViewById(R.id.editText_city)
        noResults = view.findViewById(R.id.textView_no_results)

        venueMap = HashMap()
        venueId = ""

        editTextCity.addTextChangedListener(createTextWatcher())
        editTextKeyword.addTextChangedListener(createTextWatcher())

        spinner = view.findViewById(R.id.spinner)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val venueName = parent?.getItemAtPosition(position).toString()
                venueId = venueMap[venueName] ?: ""
                Log.d(TAG, "Selected Venue: $venueName, ID: $venueId")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        login(user, loginButton, db, TAG)

        view.findViewById<Button>(R.id.button_search).setOnClickListener {
            if (editTextKeyword.text.toString().isEmpty())
                alertDialog("Search term missing", "Search term cannot be empty. Please enter a search term.")
            else if (editTextCity.text.toString().isEmpty())
                alertDialog("Location missing", "City cannot be empty. Please enter a city.")
            else {
                loginButton.visibility = View.GONE
                view.hideKeyboard()

                // Call API
                if (venueId != "") {
                    getData(editTextKeyword.text.toString(), editTextCity.text.toString(), venueId)
                } else {
                    getData(editTextKeyword.text.toString(), editTextCity.text.toString())
                }
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
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
            }
        } else {
            if (adapter.events.isNotEmpty())
                loginButton.visibility = View.GONE
        }
    }

    private fun getData(keyword: String, city: String, venueId: String? = null) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val ticketMasterAPI = retrofit.create(TicketMasterService::class.java)
        val call: Call<TicketMasterData> = if (venueId != null) {
            ticketMasterAPI.getData(apikey, keyword, city, sort, venueId)
        } else {
            ticketMasterAPI.getData(apikey, keyword, city, sort)
        }

        call.enqueue(object : Callback<TicketMasterData> {
            override fun onResponse(call: Call<TicketMasterData>, response: Response<TicketMasterData>) {
                Log.d(TAG, "OnResponse: $response")
                val body = response.body()
                if (body == null) {
                    Log.d(TAG, "Valid response was not received")
                    return
                }

                // Check and return if no events
                if (body._embedded == null || body._embedded.events.isEmpty()) {
                    widgetVisibility(0)
                    return
                }

                widgetVisibility(1)

                events.clear()
                events.addAll(body._embedded.events)

                adapter.notifyDataSetChanged()

                if (venueId == null) {
                    venueMap.clear()
                    body._embedded.events.forEach { event ->
                        event._embedded.venues.forEach { venue ->
                            venue.name?.let { name ->
                                venueMap[name] = venue.id
                            }
                        }
                    }
                    val venueNames = ArrayList<String>(venueMap.keys)
                    venueNames.add(0, "No Venue Filter")
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, venueNames)
                    spinner.adapter = adapter
                }
            }

            override fun onFailure(call: Call<TicketMasterData>, t: Throwable) {
                Log.d(TAG, "OnFailure: $t")
            }
        })
    }

    private fun alertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setIcon(android.R.drawable.ic_delete)
        builder.setPositiveButton("Okay") { _, _ ->}
        val dialog = builder.create()
        dialog.show()
    }

    private fun widgetVisibility(results: Int) {
        if (results == 0) {
            noResults.visibility = View.VISIBLE
            recyclerView.visibility = View.INVISIBLE
            spinner.visibility = View.INVISIBLE
        } else if (results == 1) {
            noResults.visibility = View.INVISIBLE
            recyclerView.visibility = View.VISIBLE
            spinner.visibility = View.VISIBLE
        }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Reset venueId when city or keyword is changed
                venueId = ""
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }

    private fun View.hideKeyboard() {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }
}
