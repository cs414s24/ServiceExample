package com.example.serviceexample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import androidx.core.content.ContextCompat
import java.util.Random


class DownloadService : Service() {

    private var stopRequested = false

    // Define static constant variables
    companion object {
        private const val CHANNEL_ID = "download_service_channel"
        private const val CHANNEL_NAME = "Download Service Channel"
        private const val TAG = "DownloadService"

    }

    // init block is executed when an instance of the class is created (i.e., when the class constructor is called).
    init {
        Log.d(TAG, "Service has initialized...")
    }

    // We don't provide binding, so return null
    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Toast.makeText(this, "Service has started...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Thread-Id: ${Thread.currentThread().id} ")

        var counter = 0

        if (intent != null) {
            val filesToDownload = intent.getStringArrayListExtra("filesToDownload")

            if (!filesToDownload.isNullOrEmpty()) {

                Thread {
                    for (file in filesToDownload) {
                        if (stopRequested) {
                            break // Exit loop if stop requested
                        }
                        Log.d(TAG, "Service is working: $file")
                        Thread.sleep(1000)


                        //Broadcast that the work is done!
                        val doneIntent = Intent()
                        doneIntent.action = "downloadComplete"
                        doneIntent.putExtra("fileName", file)
                        sendBroadcast(doneIntent, null)
                        counter++
                    }

                    // If all files are downloaded, stop the service and create a notification
                    if (counter == filesToDownload.size) {
                        // Stop the service as the service must be done at this point
                        stopSelf()

                        //Create a notification about the job is done
                        makeNotification()
                    }


                }.start()
            }
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }



    override fun onDestroy() {
        super.onDestroy()
        stopRequested = true
        Toast.makeText(this, "Service has destroyed!", Toast.LENGTH_SHORT).show()
    }

    private fun makeNotification() {

        // Create a notification channel for new Android versions
        createNotificationChannel()

        // Create the notification
        createNotification()

    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel (required for Android Oreo and above), but only on API 26+
        // because the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = "My Default Priority Channel for Test"
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun createNotification() {
        // Create the notification
        // Pending intent to be able to launch the app when the user clicks the notification
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("file",  "test") // Optional, pass data

        val flag = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
            else -> FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flag)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_circle_notifications_24)
            .setContentTitle("Test")
            .setContentText("Download is complete")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()


        // Check if the app has the required permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, perform the operation

            // Show the notification
            // notificationId is a unique int for each notification that you must define
            val notificationId = Random().nextInt()
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        }
    }

}