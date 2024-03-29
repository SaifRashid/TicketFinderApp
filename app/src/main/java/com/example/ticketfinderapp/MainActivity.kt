package com.example.ticketfinderapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.RecyclerView)

        recyclerView.adapter = MyRecyclerAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    fun searchButton (view: View) {
        val editTextKeyword = findViewById<EditText>(R.id.editText_keyword)
        val editTextCity = findViewById<EditText>(R.id.editText_city)

        if (editTextKeyword.text.toString().isEmpty())
            alertDialog("keyword")
        else if (editTextCity.text.toString().isEmpty())
            alertDialog("city")
        else
            view.hideKeyboard()
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
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

}