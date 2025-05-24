package com.example.artdecode

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView

class Settings : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val privacy = view.findViewById<CardView>(R.id.privacyPolicy)
        val terms = view.findViewById<CardView>(R.id.termsAndConditions)

        privacy.setOnClickListener {
            val privacyIntent = Intent(requireContext(), PrivacyPolicy::class.java)
            startActivity(privacyIntent)
        }

        terms.setOnClickListener {
            val termsIntent = Intent(requireContext(), Terms::class.java)
            startActivity(termsIntent)
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Settings().apply {
                arguments = Bundle().apply {
                    putString("param1", param1)
                    putString("param2", param2)
                }
            }
    }
}
