package com.example.artdecode.presentation.settings

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.example.artdecode.R
import com.example.artdecode.presentation.login.LoginActivity
import com.example.artdecode.presentation.privacy.PrivacyPolicyActivity
import com.example.artdecode.presentation.terms.TermsActivity

class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var privacy: CardView
    private lateinit var terms: CardView
    private lateinit var logout: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        initializeViews(view)
        initializeViewModel()
        setupObservers()
        setupClickListeners()

        return view
    }

    private fun initializeViews(view: View) {
        privacy = view.findViewById(R.id.privacyPolicy)
        terms = view.findViewById(R.id.termsAndConditions)
        logout = view.findViewById(R.id.logOut)
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
    }

    private fun setupObservers() {
        viewModel.logoutComplete.observe(viewLifecycleOwner) { isComplete ->
            if (isComplete) {
                navigateToLogin()
                viewModel.onLogoutHandled()
            }
        }
    }

    private fun setupClickListeners() {
        privacy.setOnClickListener {
            navigateToPrivacyPolicy()
        }

        terms.setOnClickListener {
            navigateToTermsAndConditions()
        }

        logout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun navigateToPrivacyPolicy() {
        val intent = Intent(requireContext(), PrivacyPolicyActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToTermsAndConditions() {
        val intent = Intent(requireContext(), TermsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToLogin() {
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
