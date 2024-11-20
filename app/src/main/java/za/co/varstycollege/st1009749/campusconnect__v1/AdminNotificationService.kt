package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AdminNotificationService : FirebaseMessagingService() {
    private lateinit var db: FirebaseFirestore
    private var campusQueryListener: ListenerRegistration? = null
    private var studentHubQueryListener: ListenerRegistration? = null
    private var alumniQueryListener: ListenerRegistration? = null

    private val CHANNEL_ID = "AdminUpdates"
    private val TAG = "AdminNotificationService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        initializeService()
    }

    private fun initializeService() {
        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance()

            // Create notification channel
            createNotificationChannel()

            // Start query listeners
            startQueryListeners()

            Log.d(TAG, "Service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing service: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleNow(remoteMessage.data)
        }

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title ?: "New Query", it.body ?: "You have a new query to review")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Send token to your backend if needed
    }

    private fun handleNow(data: Map<String, String>) {
        // Handle the data message here
        val title = data["title"] ?: "New Query"
        val message = data["message"] ?: "You have a new query to review"
        showNotification(title, message)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Admin Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New query notifications for admins"
                enableLights(true)
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startQueryListeners() {
        setupCollectionListener("CampusQuery", "Campus Query")
        setupCollectionListener("StudentHubQuery", "Student Hub Query")
        setupCollectionListener("AlumniQuery", "Alumni Query")
    }

    private fun setupCollectionListener(collectionName: String, displayName: String) {
        val listener = db.collection(collectionName)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed for $collectionName: ${e.message}")
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { dc ->
                    if (dc.type.name == "ADDED") {
                        val newData = dc.document.data
                        val queryName = newData["queryName"] as? String ?: "New Query"
                        val studentId = newData["studentId"] as? String ?: ""
                        val status = newData["status"] as? String ?: ""

                        if (status == "Pending") {
                            showNotification(
                                "New $displayName",
                                "New $queryName requires attention"
                            )
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

    private fun showNotification(title: String, message: String) {
        try {
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
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(longArrayOf(1000, 1000, 1000))
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)

            Log.d(TAG, "Notification sent successfully: $title - $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        campusQueryListener?.remove()
        studentHubQueryListener?.remove()
        alumniQueryListener?.remove()
        Log.d(TAG, "Service destroyed")
    }
}