package com.napjourney.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AlarmActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(0xFF102A43.toInt())
        }
        root.addView(TextView(this).apply {
            text = "WAKE UP!\nYour destination is nearby."
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
        })
        root.addView(Button(this).apply {
            text = "I'm awake — stop alarm"
            setOnClickListener {
                stopService(Intent(this@AlarmActivity, LocationAlarmService::class.java))
                finish()
            }
        })
        setContentView(root)
    }
}
