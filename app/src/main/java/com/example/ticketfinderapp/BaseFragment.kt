package com.example.ticketfinderapp

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

open class BaseFragment : Fragment() {
    fun login(user: FirebaseUser?, loginButton: Button, db: FirebaseFirestore, TAG: String) {
        // If currentUser is not null, we have a user and go back to the MainActivity
        if (user != null) {
            loginButton.visibility = View.GONE
        } else {
            loginButton.visibility = View.VISIBLE
            // create a new ActivityResultLauncher to launch the sign-in activity and handle the result
            // When the result is returned, the result parameter will contain the data and resultCode (e.g., OK, Cancelled etc.).
            val signActivityLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        // The user has successfully signed in or he/she is a new user
                        val loggedInUser = FirebaseAuth.getInstance().currentUser
                        Log.d(TAG, "onActivityResult: $loggedInUser")

                        // Checking if the user is new by checking the existence of a document with the user's UID in Firestore
                        val userRef = db.collection("favorites").document(loggedInUser!!.uid)
                        userRef.get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // This is a returning user
                                    Toast.makeText(requireView().context, "Welcome Back!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // This is a new user
                                    val docData = hashMapOf(
                                        "favoriteEvents" to arrayListOf<String>()
                                    )
                                    userRef.set(docData)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "DocumentSnapshot successfully written!")
                                            Toast.makeText(requireView().context, "Welcome New User!", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error writing document", e)
                                        }
                                }

                                // Since the user signed in, the user can go back to main activity
                                startActivity(Intent(requireView().context, MainActivity::class.java))
                                // Make sure to call finish(), otherwise the user would be able to go back to the RegisterActivity
                                requireActivity().finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error checking document existence", e)
                            }

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
                    .setLogo(R.drawable.ticket_sign_in)
                    .setIsSmartLockEnabled(false)
                    .build()

                // Launch sign-in Activity with the sign-in intent above
                signActivityLauncher.launch(signInIntent)
            }
        }
    }

    fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}