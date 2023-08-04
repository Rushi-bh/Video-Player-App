package com.rushibhosale.myvideoplayer1

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatDelegate
import com.rushibhosale.myvideoplayer1.databinding.ActivitySplashBinding
import com.rushibhosale.myvideoplayer1.databinding.RenameFieldBinding
import java.io.File

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private var splashDelay:Long =1000

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding= ActivitySplashBinding.inflate(layoutInflater)
        setTheme(R.style.coolPinkNav)
        setContentView(binding.root)
        supportActionBar?.hide()
        @Suppress("DEPRECATION")
        Handler().postDelayed(
            {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }, splashDelay
        )
    }
}