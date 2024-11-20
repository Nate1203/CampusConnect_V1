package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import android.content.pm.ServiceInfo

class NotificationService : Service() {
    private lateinit var db: FirebaseFirestore
    private var campusQueryListener: ListenerRegistration? = null
    private var studentHubQueryListener: ListenerRegistration? = null
    private var alumniQueryListener: ListenerRegistration? = null

    private val FOREGROUND_ID = 1001
    private val CHANNEL_ID = "CampusConnect_Updates"
    private val TAG = "NotificationService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        initializeService()
    }

    private fun initializeService() {
        try {
            // Initialize Firebase components
            db = FirebaseFirestore.getInstance()

            // Create notification channel
            createNotificationChannel()

            // Start as foreground service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(FOREGROUND_ID, createForegroundNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)  // Use ServiceInfo here
            } else {
                startForeground(FOREGROUND_ID, createForegroundNotification())
            }

            // Start query listeners
            startQueryListeners()

            // Get FCM token
            updateFCMToken()

            Log.d(TAG, "Service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing service: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Campus Connect Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Updates for your queries"
                enableLights(true)
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun createForegroundNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Campus Connect")
        .setContentText("Monitoring your queries")
        .setSmallIcon(R.drawable.cc_logo)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun startQueryListeners() {
        val sharedPref = getSharedPreferences("CampusConnectPrefs", Context.MODE_PRIVATE)
        val studentId = sharedPref.getString("studentId", "")

        Log.d(TAG, "Starting listeners for student: $studentId")

        if (!studentId.isNullOrEmpty()) {
            setupCollectionListener("CampusQuery", studentId)
            setupCollectionListener("StudentHubQuery", studentId)
            setupCollectionListener("AlumniQuery", studentId)
        }
    }

    private fun setupCollectionListener(collectionName: String, studentId: String) {
        val listener = db.collection(collectionName)
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed for $collectionName: ${e.message}")
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { dc ->
                    Log.d(TAG, "Change detected in $collectionName: ${dc.type}")
                    val newData = dc.document.data

                    if (dc.type.name == "MODIFIED") {
                        val status = newData["status"] as? String
                        val queryName = newData["queryName"] as? String ?: "Query"

                        Log.d(TAG, "$collectionName - Status: $status, Query: $queryName")

                        if (status == "Processing" || status == "Solved") {
                            val title = "$collectionName Update"
                            val message = "Your $queryName is now $status"
                            showNotification(title, message)
                        }
                    }
                }
            }

        when (collectionName) {
            "CampusQuery" -> campusQueryListener = listener
            "StudentHubQuery" -> studentHubQueryListener = listener
            "AlumniQuery" -> alumniQueryListener = listener
        }
    }

    private fun updateFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM Token refreshed")
                val sharedPref = getSharedPreferences("CampusConnectPrefs", Context.MODE_PRIVATE)
                val studentId = sharedPref.getString("studentId", "")

                if (!studentId.isNullOrEmpty()) {
                    db.collection("StudentProfile")
                        .document(studentId)
                        .update("fcmToken", token)
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM Token updated in Firestore")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error updating FCM token: ${e.message}")
                        }
                }
            }
    }

    private fun showNotification(title: String, message: String) {
        try {
            Log.d(TAG, "Showing notification - Title: $title, Message: $message")

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.cc_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(longArrayOf(1000, 1000, 1000))
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)

            Log.d(TAG, "Notification sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        campusQueryListener?.remove()
        studentHubQueryListener?.remove()
        alumniQueryListener?.remove()
        Log.d(TAG, "Service destroyed")
    }
}
