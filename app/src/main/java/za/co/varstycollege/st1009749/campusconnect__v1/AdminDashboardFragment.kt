package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.graphics.Color
import android.media.RingtoneManager
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot
import java.util.Date
import java.util.Locale
import android.app.Dialog
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

class AdminDashboardFragment : Fragment() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var pieChart: PieChart
    private lateinit var levelProgressBar: ProgressBar
    private lateinit var levelText: TextView
    private lateinit var experienceText: TextView
    private lateinit var queryStatusProgress: ProgressBar
    private lateinit var solvedText: TextView
    private lateinit var processingText: TextView
    private lateinit var pendingText: TextView
    private lateinit var solveQueriesButton: Button
    private lateinit var leaderboardIcon: ImageView
    private val CHANNEL_ID = "NewQueryNotifications"
    private val TAG = "AdminDashboard"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
        return view
    }

    override fun onResume() {
        super.onResume()
        setupLevelSystem()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Hide login UI
        (activity as? MainActivity)?.hideLoginUI()

        // Initialize views
        drawerLayout = view.findViewById(R.id.admin_drawer_layout)
        navigationView = view.findViewById(R.id.admin_nav_view)
        pieChart = view.findViewById(R.id.queryDistributionChart)
        queryStatusProgress = view.findViewById(R.id.queryStatusProgress)
        solvedText = view.findViewById(R.id.solvedText)
        processingText = view.findViewById(R.id.processingText)
        pendingText = view.findViewById(R.id.pendingText)
        solveQueriesButton = view.findViewById(R.id.solveQueriesButton)
        levelProgressBar = view.findViewById(R.id.levelProgressBar)
        levelText = view.findViewById(R.id.levelText)
        experienceText = view.findViewById(R.id.experienceText)
        leaderboardIcon = view.findViewById<ImageView>(R.id.leaderboardIcon)


        // Create notification channel
        createNotificationChannel()

        // Set up the Toolbar
        setupToolbar(view)

        // Fetch and update navigation header with admin info
        fetchAdminInfoAndUpdateHeader()

        // Set up navigation item selection
        setupNavigationItemSelection()

        // Set up query analytics
        setupQueryAnalytics()

        // Get FCM token
        getFCMToken()

        // Store admin login info
        storeAdminLoginInfo()

        // Set up new query listeners
        setupNewQueryListeners()

        // Set Up Level Bar
        setupLevelSystem()

        //Go to Solve Queries page
        solveQueriesButton.setOnClickListener {
            // Navigate to a new fragment or activity to handle queries
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SolveQueriesFragment())
                .addToBackStack(null)
                .commit()
        }


        leaderboardIcon.setOnClickListener {
            showLeaderboardDialog()
        }

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "New Query Notifications"
            val descriptionText = "Notifications for new incoming queries"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }


    private fun setupNewQueryListeners() {
        val collections = listOf(
            "CampusQuery" to "Campus",
            "StudentHubQuery" to "Student Hub",
            "AlumniQuery" to "Alumni"
        )

        collections.forEach { (collection, displayName) ->
            db.collection(collection)
                .whereEqualTo("status", "Pending")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen failed for $collection: ${e.message}")
                        return@addSnapshotListener
                    }

                    snapshots?.documentChanges?.forEach { dc ->
                        if (dc.type == DocumentChange.Type.ADDED) {
                            val queryData = dc.document.data
                            val queryName = queryData["queryName"] as? String ?: "New Query"

                            // Only notify if the document is new (not cached)
                            if (!dc.document.metadata.isFromCache) {
                                val title = "New $displayName Query"
                                val message = "New $queryName requires attention"
                                showNotification(title, message)
                            }
                        }
                    }
                }
        }
    }


    private fun setupLevelSystem() {
        val currentAdminEmail = auth.currentUser?.email

        if (currentAdminEmail != null) {
            // Add real-time listener for SolvedQueries
            db.collection("SolvedQueries")
                .whereEqualTo("adminEmail", currentAdminEmail)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener

                    val solvedQueries = snapshot?.size() ?: 0
                    val xp = solvedQueries * 25
                    val level = (xp / 100) + 1
                    val currentLevelXP = xp % 100

                    // Update UI
                    levelText.text = "Level $level"
                    experienceText.text = "$currentLevelXP/100 XP"
                    levelProgressBar.progress = currentLevelXP

                    // Store in AdminLevels
                    db.collection("AdminLevels")
                        .document(currentAdminEmail)
                        .set(mapOf(
                            "level" to level,
                            "currentXP" to currentLevelXP,
                            "totalSolvedQueries" to solvedQueries
                        ))
                }
        }
    }


    private data class LeaderboardEntry(
        val email: String,
        val firstName: String,
        val lastName: String,
        val level: Int,
        val xp: Int
    )


    private fun showLeaderboardDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_leaderboard)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.leaderboardRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        db.collection("AdminLevels")
            .get()
            .addOnSuccessListener { levelDocs ->
                val adminEmails = levelDocs.documents.map { it.id }

                db.collection("admins")
                    .whereIn("email", adminEmails)
                    .get()
                    .addOnSuccessListener { adminDocs ->
                        val entries = levelDocs.mapNotNull { levelDoc ->
                            val adminDoc = adminDocs.documents.find { it.getString("email") == levelDoc.id }
                            if (adminDoc != null) {
                                LeaderboardEntry(
                                    email = levelDoc.id,
                                    firstName = adminDoc.getString("firstName") ?: "",
                                    lastName = adminDoc.getString("lastName") ?: "",
                                    level = levelDoc.getLong("level")?.toInt() ?: 1,
                                    xp = levelDoc.getLong("currentXP")?.toInt() ?: 0
                                )
                            } else null
                        }.sortedByDescending { it.level }

                        recyclerView.adapter = LeaderboardAdapter(entries)
                    }
            }

        dialog.show()
    }


    private fun updateAdminLevelData(adminEmail: String) {
        db.collection("SolvedQueries")
            .whereEqualTo("adminEmail", adminEmail)
            .get()
            .addOnSuccessListener { snapshot ->
                val solvedQueries = snapshot.size()
                val xp = solvedQueries * 25
                val level = (xp / 100) + 1
                val currentLevelXP = xp % 100

                // Store level data
                db.collection("AdminLevels")
                    .document(adminEmail)
                    .set(mapOf(
                        "level" to level,
                        "currentXP" to currentLevelXP,
                        "totalSolvedQueries" to solvedQueries
                    ))
            }
    }


    private fun showNotification(title: String, message: String) {
        try {
            Log.d(TAG, "Attempting to show notification: $title - $message")

            val context = requireContext()
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

            Log.d(TAG, "Created pending intent")

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.cc_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000))
                .setContentIntent(pendingIntent)
                .build()

            Log.d(TAG, "Built notification")

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)

            Log.d(TAG, "Notification sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
            e.printStackTrace()
        }
    }


    private fun storeAdminLoginInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("admins")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
                        sharedPref?.edit()?.apply {
                            putString("loginToken", currentUser.uid)
                            putString("adminEmail", currentUser.email)
                            putString("adminFirstName", document.getString("firstName"))
                            putString("adminLastName", document.getString("lastName"))
                            putBoolean("isAdmin", true)
                            apply()
                        }
                    }
                }
        }
    }


    private inner class LeaderboardAdapter(private val entries: List<LeaderboardEntry>) :
        RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

        private val currentUserEmail = auth.currentUser?.email

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val rankText: TextView = view.findViewById(R.id.rankText)
            val adminEmail: TextView = view.findViewById(R.id.adminEmail)
            val levelText: TextView = view.findViewById(R.id.levelText)
            val xpText: TextView = view.findViewById(R.id.xpText)
            val cardView: androidx.cardview.widget.CardView = itemView as androidx.cardview.widget.CardView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.leaderboard_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            holder.rankText.text = "#${position + 1}"
            holder.adminEmail.text = "${entry.firstName} ${entry.lastName}"
            holder.levelText.text = "Level ${entry.level}"
            holder.xpText.text = "${entry.xp}/100 XP"

            if (entry.email == currentUserEmail) {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#1A7DF9FF"))
                holder.adminEmail.setTextColor(Color.parseColor("#7DF9FF"))
            } else {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#2D2D2D"))
                holder.adminEmail.setTextColor(Color.WHITE)
            }
        }

        override fun getItemCount() = entries.size
    }


    private fun setupToolbar(view: View) {
        val toolbar: Toolbar = view.findViewById(R.id.adminToolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        val menuIcon: ImageView = toolbar.findViewById(R.id.adminMenuIcon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }


    private fun setupNavigationItemSelection() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_admin_dashboard -> {
                    drawerLayout.closeDrawers()
                }
                R.id.nav_profile -> {
                    drawerLayout.closeDrawers()
                }
                R.id.nav_settings -> {
                    drawerLayout.closeDrawers()
                }
                R.id.nav_logout -> {
                    performLogout()
                }
            }
            true
        }
    }

    private fun fetchAdminInfoAndUpdateHeader() {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val email = sharedPref?.getString("adminEmail", "") ?: ""

        if (email.isNotEmpty()) {
            db.collection("admins")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val admin = documents.documents[0]
                        updateNavigationHeader(
                            admin.getString("firstName") ?: "",
                            admin.getString("lastName") ?: "",
                            email
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Error fetching admin info: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateNavigationHeader(firstName: String, lastName: String, email: String) {
        val headerView = navigationView.getHeaderView(0)
        val nameTextView = headerView.findViewById<TextView>(R.id.nav_header_name)
        val emailTextView = headerView.findViewById<TextView>(R.id.nav_header_email)
        val ratingTextView = headerView.findViewById<TextView>(R.id.nav_header_rating)

        nameTextView.text = "$firstName $lastName"
        emailTextView.text = email

        // Setup real-time rating updates
        setupRatingListener(email, ratingTextView)
    }

    private fun setupRatingListener(adminEmail: String, ratingTextView: TextView) {
        db.collection("AdminReviews")
            .whereEqualTo("adminEmail", adminEmail)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("AdminRating", "Listen failed: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    var totalRating = 0f
                    var numberOfRatings = 0

                    snapshots.documents.forEach { doc ->
                        val rating = doc.getDouble("rating")?.toFloat() ?: 0f
                        if (rating > 0) {
                            totalRating += rating
                            numberOfRatings++
                        }
                    }

                    if (numberOfRatings > 0) {
                        val averageRating = totalRating / numberOfRatings
                        val formattedRating = String.format("%.1f", averageRating)
                        activity?.runOnUiThread {
                            ratingTextView.text = "$formattedRating"
                        }
                    } else {
                        activity?.runOnUiThread {
                            ratingTextView.text = "No ratings yet"
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        ratingTextView.text = "No ratings yet"
                    }
                }
            }
    }


    private fun setupQueryAnalytics() {
        setupPieChart()
        updateQueryAnalytics()
        setupQueryStatusListener()
    }

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Queries"
            setCenterTextColor(Color.WHITE)
            legend.isEnabled = true
            legend.textColor = Color.WHITE
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    db.collection("admins")
                        .document(userId)
                        .update("fcmToken", token)
                        .addOnFailureListener { e ->
                            Log.e("FCM", "Error saving token: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Error getting FCM token: ${e.message}")
            }
    }

    private fun setupQueryStatusListener() {
        val collections = listOf("CampusQuery", "StudentHubQuery", "AlumniQuery")

        collections.forEach { collection ->
            db.collection(collection)
                .addSnapshotListener { _, e ->
                    if (e != null) {
                        Log.e("QueryListener", "Listen failed: ${e.message}")
                        return@addSnapshotListener
                    }
                    updateQueryAnalytics()
                }
        }
    }


    private fun updateQueryAnalytics() {
        val queryTasks = mutableListOf<Task<QuerySnapshot>>()

        // Add tasks for regular queries
        val collectionRefs = mapOf(
            "campus" to db.collection("CampusQuery"),
            "alumni" to db.collection("AlumniQuery"),
            "studentHub" to db.collection("StudentHubQuery")
        )

        collectionRefs.values.forEach { ref ->
            queryTasks.add(ref.get())
        }

        // Add task for SolvedQueries
        queryTasks.add(db.collection("SolvedQueries").get())

        Tasks.whenAllSuccess<QuerySnapshot>(queryTasks)
            .addOnSuccessListener { snapshots ->
                var campusQueries = 0
                var studentHubQueries = 0
                var alumniQueries = 0

                var solvedCount = 0
                var pendingCount = 0
                var processingCount = 0
                var totalQueries = 0

                // Process first three snapshots (regular query collections)
                snapshots.take(3).forEachIndexed { index, snapshot ->
                    when (index) {
                        0 -> campusQueries = snapshot.size()
                        1 -> alumniQueries = snapshot.size()
                        2 -> studentHubQueries = snapshot.size()
                    }

                    snapshot.documents.forEach { doc ->
                        when (doc.getString("status")) {
                            "Pending" -> pendingCount++
                            "Processing" -> processingCount++
                            // Don't count "Solved" here anymore
                        }
                    }
                    // Only add pending and processing queries to total
                    totalQueries += snapshot.documents.count { doc ->
                        val status = doc.getString("status")
                        status == "Pending" || status == "Processing"
                    }
                }

                // Process SolvedQueries collection (last snapshot)
                val solvedSnapshot = snapshots.last()
                solvedCount = solvedSnapshot.size()
                totalQueries += solvedCount // Add solved queries to total

                if (totalQueries > 0) {
                    // Update pie chart with percentages
                    val entries = mutableListOf<PieEntry>()

                    if (campusQueries > 0) entries.add(PieEntry(campusQueries.toFloat(), "Campus"))
                    if (alumniQueries > 0) entries.add(PieEntry(alumniQueries.toFloat(), "Alumni"))
                    if (studentHubQueries > 0) entries.add(PieEntry(studentHubQueries.toFloat(), "Student Hub"))

                    val dataSet = PieDataSet(entries, "Query Types")
                    dataSet.colors = listOf(
                        Color.parseColor("#7DF9FF"),
                        Color.parseColor("#6f42c1"),
                        Color.parseColor("#e83e8c")
                    )
                    dataSet.valueTextColor = Color.WHITE
                    dataSet.valueTextSize = 12f

                    val pieData = PieData(dataSet)
                    pieChart.data = pieData
                    pieChart.invalidate()

                    // Update status bar and percentages
                    val solvedPercentage = "%.1f".format(solvedCount.toFloat() / totalQueries * 100)
                    val processingPercentage = "%.1f".format(processingCount.toFloat() / totalQueries * 100)
                    val pendingPercentage = "%.1f".format(pendingCount.toFloat() / totalQueries * 100)

                    solvedText.text = "Solved\n$solvedPercentage%"
                    processingText.text = "Processing\n$processingPercentage%"
                    pendingText.text = "Pending\n$pendingPercentage%"

                    queryStatusProgress.max = 100
                    queryStatusProgress.progress = (solvedCount.toFloat() / totalQueries * 100).toInt()
                    queryStatusProgress.secondaryProgress = ((solvedCount + processingCount).toFloat() / totalQueries * 100).toInt()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("QueryAnalytics", "Error getting query data: ${exception.message}")
                Toast.makeText(context, "Error loading query data", Toast.LENGTH_SHORT).show()
            }
    }


    private fun performLogout() {
        try {
            // Sign out from Firebase Auth
            auth.signOut()

            // Stop the query listener service
            activity?.stopService(Intent(activity, AdminNotificationService::class.java))  // Changed to AdminNotificationService

            // Clear SharedPreferences
            val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
            sharedPref?.edit()?.clear()?.apply()

            // Navigate back to the login screen
            activity?.let {
                val intent = Intent(it, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                it.finish()
            }

            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("Logout", "Error during logout: ${e.message}")
            Toast.makeText(context, "Error during logout. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }







}