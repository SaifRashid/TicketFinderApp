package com.example.ticketfinderapp.ui.settings

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
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ticketfinderapp.BaseFragment
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

class SettingsFragment : BaseFragment() {
    private val TAG = "Settings Fragment"

    private lateinit var list: List<String>
    private lateinit var settingsAdapter: ArrayAdapter<String>
    private lateinit var settingsListView: ListView

    private lateinit var loginButton: Button
    private lateinit var favoriteEvents: ArrayList<String>

    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        // Write your code
        list = listOf("Sign Out", "Remove All Favorited Events", "Delete Account")

        settingsAdapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, list)
        settingsListView = view.findViewById(R.id.listView_settings)
        settingsListView.adapter = settingsAdapter

        db = FirebaseFirestore.getInstance()
        loginButton = view.findViewById(R.id.login_button2)
        val user = FirebaseAuth.getInstance().currentUser
        favoriteEvents = ArrayList()

        login(user, loginButton, db, TAG)

        settingsListView.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                // Sign out
                if (user != null) {
                    showToast("Successfully signed out")
                    AuthUI.getInstance().signOut(view.context)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // After logout, start the MainActivity again
                                startActivity(Intent(view.context, MainActivity::class.java))
                                requireActivity().finish()
                            } else {
                                showToast("Sign out failed")
                            }
                        }
                } else {
                    showToast("You are not signed in")
                }
            } else if (position == 1) {
                // Remove all favorite events
                if (user != null) {
                    val favoritesCollection = db.collection("favorites").document(user.uid)
                    favoritesCollection.update("favoriteEvents", favoriteEvents)
                        .addOnSuccessListener {
                            showToast("All events removed from favorites")
                        }
                        .addOnFailureListener {
                            showToast("Failed to update favorites")
                        }
                } else {
                    showToast("Please sign in to manage your favorites")
                }
            } else if (position == 2) {
                // Delete account
                if (user != null) {
                    // Create an alertdialog builder object,
                    // then set attributes that you want the dialog to have
                    val builder = AlertDialog.Builder(view.context)
                    builder.setTitle("Account Deletion")
                    builder.setMessage("Are you sure that you want to delete your account?\n\nThis action cannot be undone.")
                    // Set an icon, optional
                    builder.setIcon(R.drawable.baseline_delete_forever_24)
                    // Set the button actions (i.e. listeners), optional
                    builder.setPositiveButton("YES") { _, _ ->
                        db.collection("favorites").document(user.uid).delete()
                        AuthUI.getInstance().delete(view.context)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // After Delete, start the MainActivity again
                                    startActivity(Intent(view.context, MainActivity::class.java))
                                    requireActivity().finish()
                                } else {
                                    Log.e(TAG, "Task is not successful:${task.exception}")
                                }
                            }
                    }
                    builder.setNegativeButton("NO") { _, _ ->

                    }
                    // create the dialog and show it
                    val dialog = builder.create()
                    dialog.show()
                } else {
                    showToast("You are not signed in")
                }
            }
        }

        return view
    }
}

