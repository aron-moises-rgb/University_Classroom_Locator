package com.example.oopproject.frontEnd

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.oopproject.R
import com.example.oopproject.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var profileListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadUserProfileRealtime()

        // Sign Out button click with confirmation dialog
        binding.btnSignOut.setOnClickListener {
            confirmSignOut()
        }
    }

    private fun loadUserProfileRealtime() {
        val user = auth.currentUser
        if (user == null) {
            binding.tvProfileHeaderName.text = getString(R.string.not_signed_in)
            binding.tvProfileName.text = "N/A"
            binding.tvProfileEmail.text = "N/A"
            binding.tvProfileAccountType.text = "N/A"
            return
        }

        // Show Firebase Auth email initially
        binding.tvProfileEmail.text = user.email ?: ""
        binding.tvProfileHeaderName.text = user.email?.substringBefore("@") ?: "User"

        // Listen to User Profile changes in Firestore in real-time
        profileListener = db.collection("users")
            .document(user.uid)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("ProfileFragment", "Firestore error: ${error.message}")
                    return@addSnapshotListener
                }

                if (_binding != null && document != null && document.exists()) {
                    val fullName = document.getString("fullName") ?: document.getString("name") ?: "N/A"
                    val email = document.getString("email") ?: user.email ?: "N/A"
                    val accountType = document.getString("accountType") ?: "Student"

                    binding.tvProfileHeaderName.text = fullName
                    binding.tvProfileName.text = fullName
                    binding.tvProfileEmail.text = email

                    val formattedRole = when (accountType.lowercase()) {
                        "admin" -> "🛡️ Admin (System Administrator)"
                        "teacher", "faculty" -> "👨‍🏫 Teacher / Faculty"
                        else -> "🎓 Student"
                    }
                    binding.tvProfileAccountType.text = formattedRole
                }
            }
    }

    private fun confirmSignOut() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out of your account?")
            .setPositiveButton("Sign Out") { _, _ ->
                auth.signOut()
                val intent = Intent(requireContext(), SignInPage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileListener?.remove()
        _binding = null
    }
}
