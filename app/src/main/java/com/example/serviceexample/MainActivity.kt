package com.example.serviceexample

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    // to keep track whether the service is running
    private var serviceIsRunning = false

    // Pretend these files are urls that the user wants to download
    private val listOfFakeFileNames = arrayListOf("File-1", "File-2", "File-3", "File-4", "File-5",
        "File-6", "File-7", "File-8", "File-9", "File-10")
    

    //  an instance of the DownloadReceiver
    private var downloadReceiver: DownloadReceiver? = null

    private lateinit var myIntent: Intent

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ask for POST_NOTIFICATIONS permission (which is needed to create notifications)
        checkPermission()

    }

    fun startServiceButton(view: View) {

        if (serviceIsRunning) {
            Log.d(TAG, "Service is currently running, please wait until it is done")
            return
        }

        // Register the broadcast receiver
        registerMyReceiver()
        Log.d(TAG, "Receiver registered")

        myIntent = Intent(this, DownloadService::class.java)
        myIntent.putStringArrayListExtra("filesToDownload", listOfFakeFileNames)
        startService(myIntent) // not startActivity


        // Show a static text
        findViewById<TextView>(R.id.tv_downloading).text = "Downloading..."
        serviceIsRunning = true
    }




    private inner class DownloadReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            // Handle the received broadcast message
            Log.d(TAG, "DownloadReceiver: Received broadcast!")

            val file = intent.getStringExtra("fileName")
            Log.d(TAG, "DownloadReceiver: $file has been downloaded")

            // Update the UI, by running the code below in UI Thread
            runOnUiThread {

                val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
                progressBar.progress += 10

                val progress = progressBar.progress / listOfFakeFileNames.size

                if (progress == listOfFakeFileNames.size) {
                    findViewById<TextView>(R.id.tv_downloading).text = "Download has completed!"
                } else {
                    // This will look like : Downloading ... 1/10 etc.
                    findViewById<TextView>(R.id.tv_downloading).text = "Downloading... $progress / ${listOfFakeFileNames.size}"
                }
            }

        }
    }



    private fun registerMyReceiver() {
        // Creating an IntentFilter to listen for a specific action
        val filter = IntentFilter()
        // Adding an action to the IntentFilter
        filter.addAction("downloadComplete")
        downloadReceiver = DownloadReceiver()


        // For apps running on Android 14 or higher, it is required to register receivers using the
        // RECEIVER_EXPORTED/RECEIVER_NOT_EXPORTED flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Registering a BroadcastReceiver with a specified IntentFilter and export permission
            registerReceiver(downloadReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, filter)
        }
    }



    //  unregister the broadcast receiver after this activity is paused
    override fun onPause() {
        super.onPause()
        if (downloadReceiver != null) {
            Log.d(TAG, "Unregistering the broadcast receiver")
            unregisterReceiver(downloadReceiver)
            downloadReceiver = null
            // Stop the service with the intent
            stopService(myIntent)
        }
        serviceIsRunning = false
    }



    private fun checkPermission() {
        // Check if the app has the required permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            Log.d(TAG, "Permission has not been granted.")

            // Check if the device's SDK version is Tiramisu or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Define a request code for the permission request (this is an arbitrary integer value)
                val requestCode = 200
                // Request permission to post notifications
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
            }

        }
    }

}