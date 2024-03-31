package com.example.ticketfinderapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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

    private val BASE_URL = "https://app.ticketmaster.com/discovery/v2/events/"
    private val apikey = "Ayl9reEX22B94IUU01mLxM89kAE1H6ia"
    private val sort = "date,asc"
    private val TAG = "MainActivity"

    private var events = ArrayList<Event>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById<RecyclerView>(R.id.RecyclerView)
        adapter = MyRecyclerAdapter(events)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    fun searchButton(view: View) {
        val editTextKeyword = findViewById<EditText>(R.id.editText_keyword)
        val editTextCity = findViewById<EditText>(R.id.editText_city)

        if (editTextKeyword.text.toString().isEmpty())
            alertDialog("keyword")
        else if (editTextCity.text.toString().isEmpty())
            alertDialog("city")
        else {
            view.hideKeyboard()

            // Call API
            getData(editTextKeyword.text.toString(), editTextCity.text.toString())
        }
    }

    private fun getData(keyword: String, city: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val ticketMasterAPI = retrofit.create(TicketMasterService::class.java)
        ticketMasterAPI.getData(apikey, keyword, city, sort).enqueue(object : Callback<TicketMasterData> {
            override fun onResponse(call: Call<TicketMasterData>, response: Response<TicketMasterData>) {
                Log.d(TAG, "OnResponse: $response")
                val body = response.body()
                if (body == null) {
                    Log.d(TAG, "Valid response was not received")
                    return
                }

                val noResults = findViewById<TextView>(R.id.textView_no_results)
                // Check and return if no events
                if (body._embedded == null || body._embedded.events.isEmpty()) {
                    noResults.visibility = View.VISIBLE
                    recyclerView.visibility = View.INVISIBLE
                    return
                }

                noResults.visibility = View.INVISIBLE
                recyclerView.visibility = View.VISIBLE

                events.clear()
                events.addAll(body._embedded.events)
                adapter.notifyDataSetChanged()
            }

            override fun onFailure(call: Call<TicketMasterData>, t: Throwable) {
                Log.d(TAG, "OnFailure: $t")
            }
        })
    }

    private fun alertDialog(missing: String) {
        val title: String
        val message: String

        if (missing == "keyword") {
            title = "Search term missing"
            message = "Search term cannot be empty. Please enter a search term."
        } else {
            title = "Location missing"
            message = "City cannot be empty. Please enter a city."
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setIcon(android.R.drawable.ic_delete)
        builder.setPositiveButton("Okay") { _, _ ->}
        val dialog = builder.create()
        dialog.show()
    }

    private fun View.hideKeyboard() {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

}