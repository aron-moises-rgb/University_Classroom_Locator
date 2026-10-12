package com.example.oopproject

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class HomePage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        auth = FirebaseAuth.getInstance()

        val root = findViewById<LinearLayout>(R.id.main)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val btnSignOut = findViewById<MaterialButton>(R.id.btnSignOut)

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Display current user's email
        tvUserEmail.text = auth.currentUser?.email ?: "No user"

        // Sign out
        btnSignOut.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, sign_in_page::class.java))
            finish()
        }
    }
}