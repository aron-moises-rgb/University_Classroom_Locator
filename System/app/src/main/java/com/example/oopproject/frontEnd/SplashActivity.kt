package com.example.oopproject.frontEnd

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.oopproject.R
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val ivLogo = findViewById<ImageView>(R.id.ivLogo)

        // Apply smooth scale and fade-in animation to Logo
        val animSet = AnimationSet(true).apply {
            addAnimation(AlphaAnimation(0.0f, 1.0f).apply { duration = 1200 })
            addAnimation(
                ScaleAnimation(
                    0.8f, 1.0f, 0.8f, 1.0f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                ).apply { duration = 1200 }
            )
        }
        ivLogo.startAnimation(animSet)

        // 2-Second Delay before routing
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationAndNavigate()
        }, 2000)
    }

    private fun checkAuthenticationAndNavigate() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, HomePage::class.java))
        } else {
            startActivity(Intent(this, SignInPage::class.java))
        }
        finish()
    }
}
