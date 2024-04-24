package com.example.ticketfinderapp.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ticketfinderapp.Event
import com.example.ticketfinderapp.MainActivity
import com.example.ticketfinderapp.MyRecyclerAdapter
import com.example.ticketfinderapp.R
import com.example.ticketfinderapp.TicketMasterData
import com.example.ticketfinderapp.TicketMasterService
import com.example.ticketfinderapp.databinding.FragmentHomeBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragment : Fragment() {
    private val BASE_URL = "https://app.ticketmaster.com/discovery/v2/"
    private val apikey = "Ayl9reEX22B94IUU01mLxM89kAE1H6ia"
    private val sort = "date,asc"
    private val TAG = "Home Fragment"

    private lateinit var loginButton: Button

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
        var db = FirebaseFirestore.getInstance()
        loginButton = view.findViewById(R.id.login_button)

        recyclerView = view.findViewById(R.id.RecyclerView)
        adapter = MyRecyclerAdapter(events)
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
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val venueName = parent?.getItemAtPosition(position).toString()
                venueId = venueMap[venueName] ?: ""
                Log.d(TAG, "Selected Venue: $venueName, ID: $venueId")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Get instance of the FirebaseAuth
        val currentUser = FirebaseAuth.getInstance().currentUser

        // If currentUser is not null, we have a user and go back to the MainActivity
        if (currentUser != null) {
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
                            Toast.makeText(view.context, "Welcome New User!", Toast.LENGTH_SHORT)
                                .show()
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

        view.findViewById<Button>(R.id.button_search).setOnClickListener {
            if (editTextKeyword.text.toString().isEmpty())
                alertDialog("Search term missing", "Search term cannot be empty. Please enter a search term.")
            else if (editTextCity.text.toString().isEmpty())
                alertDialog("Location missing", "City cannot be empty. Please enter a city.")
            else {
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
/*    fun searchButton(view: View) {
        if (editTextKeyword.text.toString().isEmpty())
            alertDialog("Search term missing", "Search term cannot be empty. Please enter a search term.")
        else if (editTextCity.text.toString().isEmpty())
            alertDialog("Location missing", "City cannot be empty. Please enter a city.")
        else {
            view.hideKeyboard()

            // Call API
            if (venueId != "") {
                getData(editTextKeyword.text.toString(), editTextCity.text.toString(), venueId)
            } else {
                getData(editTextKeyword.text.toString(), editTextCity.text.toString())
            }
        }
    }*/

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
