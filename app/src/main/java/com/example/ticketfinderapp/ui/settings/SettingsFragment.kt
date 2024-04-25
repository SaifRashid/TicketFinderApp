package com.example.ticketfinderapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.ticketfinderapp.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        val textView: TextView = view.findViewById(R.id.text_settings)
        settingsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return view
    }
}
