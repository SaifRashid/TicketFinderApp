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

class SettingsFragment : Fragment() {
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
                                    Toast.makeText(
                                        view.context,
                                        "Welcome New User!",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
            loginButton.setOnClickListener {
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

        settingsListView.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                // Sign out
                if (user != null) {
                    Toast.makeText(view.context, "Logout", Toast.LENGTH_SHORT).show()
                    AuthUI.getInstance().signOut(view.context)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // After logout, start the MainActivity again
                                startActivity(Intent(view.context, MainActivity::class.java))
                                requireActivity().finish()
                            } else {
                                Log.e(TAG, "Task is not successful:${task.exception}")
                            }
                        }
                } else {
                    Toast.makeText(view.context, "You are not logged in", Toast.LENGTH_SHORT).show()
                }
            } else if (position == 1) {
                // Remove all favorite events
                if (user != null) {
                    val favoritesCollection = db.collection("favorites").document(user.uid)
                    favoritesCollection.update("favoriteEvents", favoriteEvents)
                        .addOnSuccessListener {
                            Toast.makeText(
                                view.context,
                                "All events removed from favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                view.context,
                                "Failed to update favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(
                        view.context,
                        "Please sign in manage your favorites",
                        Toast.LENGTH_SHORT
                    ).show()
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
                    builder.setPositiveButton("YES"){ dialog, which ->
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
                    builder.setNegativeButton("NO"){ dialog, which ->

                    }
                    // create the dialog and show it
                    val dialog = builder.create()
                    dialog.show()
                } else {
                    Toast.makeText(view.context, "You are not logged in", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser

}
    }

