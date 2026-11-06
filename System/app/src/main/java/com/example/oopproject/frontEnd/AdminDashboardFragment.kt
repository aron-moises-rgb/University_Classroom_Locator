package com.example.oopproject.frontEnd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.oopproject.R
import com.google.android.material.button.MaterialButton

class AdminDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGoBuildings: MaterialButton = view.findViewById(R.id.btnGoBuildings)
        val btnGoClassrooms: MaterialButton = view.findViewById(R.id.btnGoClassrooms)
        val btnGoSchedules: MaterialButton = view.findViewById(R.id.btnGoSchedules)

        btnGoBuildings.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Buildings Manager", Toast.LENGTH_SHORT).show()
        }

        btnGoClassrooms.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Classrooms Manager", Toast.LENGTH_SHORT).show()
        }

        btnGoSchedules.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Schedules Manager", Toast.LENGTH_SHORT).show()
        }
    }
}
