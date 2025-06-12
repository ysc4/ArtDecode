package com.example.artdecode

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth

class Settings : Fragment() {

    private lateinit var auth: FirebaseAuth // Declare FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val privacy = view.findViewById<CardView>(R.id.privacyPolicy)
        val terms = view.findViewById<CardView>(R.id.termsAndConditions)
        val logout = view.findViewById<CardView>(R.id.logOut) // Get the logout button

        privacy.setOnClickListener {
            val privacyIntent = Intent(requireContext(), PrivacyPolicy::class.java)
            startActivity(privacyIntent)
        }

        terms.setOnClickListener {
            val termsIntent = Intent(requireContext(), Terms::class.java)
            startActivity(termsIntent)
        }

        logout.setOnClickListener {
            performLogout()
        }

        return view
    }

    private fun performLogout() {
        // Sign out from Firebase
        auth.signOut()

        // Optional: If you are also using Google Sign-In and want to clear its state
        // You might need to add GoogleSignInClient logic here if you want a full sign out
        // from the Google account on the device, not just from Firebase.
        // For just Firebase logout, auth.signOut() is usually sufficient.

        // Navigate back to the Login screen
        // Ensure LoginActivity is the correct name of your login activity
        val intent = Intent(requireActivity(), Login::class.java)
        // Add flags to clear the back stack and start a new task
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish() // Finish the current activity (hosting the fragment)
    }
}