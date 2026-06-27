package com.example.datetimenotification

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    companion object {
        const val CHANNEL_ID = "schedule_channel"
        const val NOTIFICATION_ID = 1001
        const val PERMISSION_REQUEST_CODE = 100
    }

    private var selectedYear = -1
    private var selectedMonth = -1
    private var selectedDay = -1
    private var selectedHour = -1
    private var selectedMinute = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        val btnPickDate = findViewById<Button>(R.id.btnPickDate)
        val btnPickTime = findViewById<Button>(R.id.btnPickTime)
        val btnNotify = findViewById<Button>(R.id.btnNotify)
        val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)
        val tvSelectedTime = findViewById<TextView>(R.id.tvSelectedTime)
        val tvScheduled = findViewById<TextView>(R.id.tvScheduled)

        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month
                    selectedDay = dayOfMonth
                    tvSelectedDate.text = "Date: %02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnPickTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute
                    val amPm = if (hour < 12) "AM" else "PM"
                    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                    tvSelectedTime.text = "Time: %02d:%02d %s".format(displayHour, minute, amPm)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        btnNotify.setOnClickListener {
            if (selectedYear == -1) {
                Toast.makeText(this, "Please select a date first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedHour == -1) {
                Toast.makeText(this, "Please select a time first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        PERMISSION_REQUEST_CODE
                    )
                    return@setOnClickListener
                }
            }

            sendScheduleNotification()

            val amPm = if (selectedHour < 12) "AM" else "PM"
            val displayHour = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
            tvScheduled.text = "✅ Notification sent for:\n%02d/%02d/%04d at %02d:%02d %s".format(
                selectedDay, selectedMonth + 1, selectedYear,
                displayHour, selectedMinute, amPm
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Schedule Notifications"
            val descriptionText = "Notifications for scheduled date and time reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendScheduleNotification() {
        val amPm = if (selectedHour < 12) "AM" else "PM"
        val displayHour = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
        val scheduleDetails = "Scheduled for: %02d/%02d/%04d at %02d:%02d %s".format(
            selectedDay, selectedMonth + 1, selectedYear,
            displayHour, selectedMinute, amPm
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📅 Schedule Confirmed!")
            .setContentText(scheduleDetails)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your schedule has been set!\n\n$scheduleDetails")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, notification)
        }

        Toast.makeText(this, "Notification sent!", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendScheduleNotification()
            } else {
                Toast.makeText(this, "Notification permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
