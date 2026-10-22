package com.example.oopproject.frontEnd

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.oopproject.R
import com.example.oopproject.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        loadUserProfile()

        // Sign Out button click
        binding.btnSignOut.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), SignInPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun loadUserProfile() {
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

        // Fetch user data from Firestore
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    val fullName = document.getString("fullName") ?: "N/A"
                    val email = document.getString("email") ?: user.email ?: "N/A"
                    val accountType = document.getString("accountType") ?: "N/A"

                    binding.tvProfileHeaderName.text = fullName
                    binding.tvProfileName.text = fullName
                    binding.tvProfileEmail.text = email
                    binding.tvProfileAccountType.text = accountType
                } else {
                    binding.tvProfileName.text = "N/A"
                    binding.tvProfileAccountType.text = "N/A"
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Failed to load profile data", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
