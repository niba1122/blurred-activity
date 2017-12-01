package com.example.blurredactivity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BlurredModalActivity.AsyncCapturer.Capturable {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openButton.setOnClickListener {
            BlurredModalActivity.AsyncCapturer(this).capture()
        }

        val view: View = window.findViewById(android.R.id.content)
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            view.draw(c)
        } catch (e: IllegalArgumentException) {
        }
    }

    override fun onCapture(result: Uri?) {
        startActivity(
                Intent(this, BlurredModalActivity::class.java).apply {
                    putExtra("capture_uri", result)
                }
        )
    }
}

