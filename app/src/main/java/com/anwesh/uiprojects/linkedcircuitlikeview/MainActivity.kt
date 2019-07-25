package com.anwesh.uiprojects.linkedcircuitlikeview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fullScreen()
    }

    fun MainActivity.fullScreen() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}