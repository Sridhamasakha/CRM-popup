package com.hkmcrm.popup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import kotlin.concurrent.thread

class MainActivity : android.app.Activity() {

    private lateinit var keyField: EditText
    private lateinit var emailField: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            buildUi()
        } catch (t: Throwable) {
            val box = TextView(this)
            box.text = "HKM Caller could not start.\n\n" + t.stackTraceToString().take(1200)
            box.textSize = 12f
            setContentView(box)
        }
    }

    private fun buildUi() {

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "HKM Caller"
            textSize = 22f
        })
        root.addView(TextView(this).apply {
            text = "Shows CRM details when a saved donor or lead calls, and logs the call back to the CRM."
        })

        root.addView(TextView(this).apply { text = "\nDevice key" })
        keyField = EditText(this).apply {
            hint = "Paste the device key from CRM settings"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(Prefs.deviceKey(this@MainActivity))
        }
        root.addView(keyField)

        root.addView(TextView(this).apply { text = "\nYour CRM login email" })
        emailField = EditText(this).apply {
            hint = "you@example.com"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(Prefs.userEmail(this@MainActivity))
        }
        root.addView(emailField)

        root.addView(Switch(this).apply {
            text = "Watch calls"
            isChecked = Prefs.isEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, value ->
                Prefs.setEnabled(this@MainActivity, value)
                if (value) startWatch() else stopService(Intent(this@MainActivity, CallWatchService::class.java))
            }
        })

        root.addView(Button(this).apply {
            text = "Save and test"
            setOnClickListener {
                Prefs.setDeviceKey(this@MainActivity, keyField.text.toString())
                Prefs.setUserEmail(this@MainActivity, emailField.text.toString())
                status.text = "Checking..."
                thread {
                    val hit = Crm.lookup(this@MainActivity, "9959645146")
                    runOnUiThread {
                        status.text = when {
                            hit == null -> "Could not reach the CRM. Check the device key and internet."
                            hit.matched -> "Connected. Test number matched: ${hit.name}"
                            else -> "Connected, but the test number is not in the CRM."
                        }
                    }
                }
            }
        })

        root.addView(Button(this).apply {
            text = "Grant permissions"
            setOnClickListener { askPermissions() }
        })

        status = TextView(this)
        root.addView(status)

        setContentView(ScrollView(this).apply { addView(root) })
        askPermissions()
    }

    private fun startWatch() {
        try {
            val svc = Intent(this, CallWatchService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)
        } catch (t: Throwable) {
            status.text = "Could not start call watching: " + (t.message ?: "unknown error")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val missing = permissions.filterIndexed { i, _ -> grantResults.getOrNull(i) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty() && Prefs.isEnabled(this)) startWatch()
    }

    private fun askPermissions() {
        try {
            val needed = mutableListOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) needed.add(Manifest.permission.POST_NOTIFICATIONS)
            val missing = needed.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 7)
        } catch (t: Throwable) {
            status.text = "Permission request failed: " + (t.message ?: "")
        }
    }
}
