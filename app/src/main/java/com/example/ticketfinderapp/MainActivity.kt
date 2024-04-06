package com.example.ticketfinderapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private val BASE_URL = "https://app.ticketmaster.com/discovery/v2/"
    private val apikey = "Ayl9reEX22B94IUU01mLxM89kAE1H6ia"
    private val sort = "date,asc"
    private val TAG = "MainActivity"

    private val events = ArrayList<Event>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyRecyclerAdapter
    private lateinit var editTextKeyword: EditText
    private lateinit var editTextCity: EditText
    private lateinit var noResults: TextView

    private lateinit var spinner: Spinner
    private lateinit var venueMap: HashMap<String, String>
    private lateinit var venueId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.RecyclerView)
        adapter = MyRecyclerAdapter(events)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        editTextKeyword = findViewById(R.id.editText_keyword)
        editTextCity = findViewById(R.id.editText_city)
        noResults = findViewById(R.id.textView_no_results)

        venueMap = HashMap()
        venueId = ""

        editTextCity.addTextChangedListener(createTextWatcher())
        editTextKeyword.addTextChangedListener(createTextWatcher())

        spinner = findViewById(R.id.spinner)
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
    }

    fun searchButton(view: View) {
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
                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, venueNames)
                    spinner.adapter = adapter
                }
            }

            override fun onFailure(call: Call<TicketMasterData>, t: Throwable) {
                Log.d(TAG, "OnFailure: $t")
            }
        })
    }

    private fun alertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
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