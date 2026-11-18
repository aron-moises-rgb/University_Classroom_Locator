package com.example.oopproject.frontEnd

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.oopproject.R
import com.example.oopproject.backEnd.constants.CampusBuildings
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminDashboardFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var classroomsListener: ListenerRegistration? = null
    private var schedulesListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGoClassrooms: MaterialButton = view.findViewById(R.id.btnGoClassrooms)
        val btnGoSchedules: MaterialButton = view.findViewById(R.id.btnGoSchedules)
        val tvAdminClassroomsCount: TextView = view.findViewById(R.id.tvAdminClassroomsCount)
        val tvAdminSchedulesCount: TextView = view.findViewById(R.id.tvAdminSchedulesCount)
        val tvAdminBuildingsCount: TextView = view.findViewById(R.id.tvAdminBuildingsCount)

        tvAdminBuildingsCount.text = CampusBuildings.COUNT.toString()

        // Real-time Classrooms Count
        classroomsListener = db.collection("classrooms").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("AdminDashboard", "Classrooms error: ${e.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && isAdded) {
                tvAdminClassroomsCount.text = snapshot.size().toString()
            }
        }

        // Real-time Schedules Count
        schedulesListener = db.collection("schedules").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("AdminDashboard", "Schedules error: ${e.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && isAdded) {
                tvAdminSchedulesCount.text = snapshot.size().toString()
            }
        }

        btnGoClassrooms.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.replaceFragment(ManageClassroomsFragment())
        }

        btnGoSchedules.setOnClickListener {
            val homeActivity = activity as? HomePage
            homeActivity?.replaceFragment(ManageSchedulesFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        classroomsListener?.remove()
        schedulesListener?.remove()
    }
}
