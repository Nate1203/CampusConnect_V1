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
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale
import android.graphics.BitmapFactory
import android.util.Base64
import de.hdodenhof.circleimageview.CircleImageView

class StudentDashboardFragment : Fragment() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var gridLayout: GridLayout
    private lateinit var gridLayout1: GridLayout
    private lateinit var alumniQueriesGridLayout: GridLayout
    private lateinit var searchView: SearchView
    private lateinit var notificationDot: View
    private lateinit var solvedQueriesIcon: ImageView
    private lateinit var bellIcon: ImageView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("LifecycleCheck", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_student_dashboard, container, false)
        Log.d("LifecycleCheck", "View inflated")
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("LifecycleCheck", "onViewCreated called")

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get FCM token
        getFCMToken()

        // Initialize components
        drawerLayout = view.findViewById(R.id.drawer_layout)
        navigationView = view.findViewById(R.id.nav_view)
        gridLayout = view.findViewById(R.id.gridLayout)
        gridLayout1 = view.findViewById(R.id.gridLayout1)
        alumniQueriesGridLayout = view.findViewById(R.id.alumniQueriesGridLayout)
        searchView = view.findViewById(R.id.searchView)

        // Set up the Toolbar
        setupToolbar(view)

        // Fetch and update navigation header with student info
        fetchStudentInfoAndUpdateHeader()

        // Set up navigation item selection
        setupNavigationItemSelection()

        // Set up query cards
        setupCampusQueryCards()
        setupStudentHubQueryCards()
        setupAlumniQueryCards()

        // Set up search functionality
        setupSearchView()

        // Start the query listener service
        activity?.startService(Intent(activity, NotificationService::class.java))

        view.findViewById<Button>(R.id.viewPreviousQueriesButton).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PreviousQueriesFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupToolbar(view: View) {
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        val menuIcon: ImageView = toolbar.findViewById(R.id.menuIcon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }


    private fun setupQueryStatusListener() {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val studentId = sharedPref?.getString("studentId", "") ?: ""

        Log.d("NotificationDebug", "Setting up listener - StudentId: $studentId")

        if (studentId.isNotEmpty()) {
            db.collection("queries")
                .whereEqualTo("studentId", studentId)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("NotificationDebug", "Listen failed: ${e.message}")
                        return@addSnapshotListener
                    }

                    Log.d("NotificationDebug", "Snapshot received - count: ${snapshots?.documentChanges?.size ?: 0}")

                    snapshots?.documentChanges?.forEach { dc ->
                        Log.d("NotificationDebug", "Document data: ${dc.document.data}")
                        Log.d("NotificationDebug", "Change type: ${dc.type}")

                        if (dc.type == DocumentChange.Type.MODIFIED) {
                            val docData = dc.document.data
                            val status = docData["status"] as? String
                            val queryName = docData["queryName"] as? String
                            val queryCategory = docData["queryCategory"] as? String

                            Log.d("NotificationDebug", """
                            Document changed:
                            - Status: $status
                            - Query Name: $queryName
                            - Category: $queryCategory
                            - StudentId: ${docData["studentId"]}
                        """.trimIndent())

                            if (status == "Processing" || status == "Solved") {
                                val title = "$queryCategory Status Update"
                                val message = when (status) {
                                    "Processing" -> "Your $queryName is now being processed"
                                    "Solved" -> {
                                        notificationDot.visibility = View.VISIBLE
                                        sharedPref?.edit()?.putBoolean("hasNewSolvedQueries", true)?.apply()
                                        "Your $queryName has been resolved"
                                    }
                                    else -> "Your $queryName status has been updated to: $status"
                                }

                                Log.d("NotificationDebug", "Showing notification for $queryName with status $status")
                                showNotification(title, message)
                            }
                        }
                    }
                }
        } else {
            Log.e("NotificationDebug", "No studentId found in SharedPreferences")
        }
    }


    private fun checkForUnreadNotifications() {
        val hasNewSolvedQueries = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
            ?.getBoolean("hasNewSolvedQueries", false) ?: false
        notificationDot.visibility = if (hasNewSolvedQueries) View.VISIBLE else View.GONE
    }

    private fun showNotification(title: String, message: String) {
        val context = context ?: return
        Log.d("NotificationDebug", "Showing notification: $title - $message")

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "QueryUpdates"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Query Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Status updates for your queries"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.cc_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .build()

            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)
            Log.d("NotificationDebug", "Notification shown successfully with ID: $notificationId")

        } catch (e: Exception) {
            Log.e("NotificationDebug", "Error showing notification: ${e.message}", e)
        }
    }





    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                // Store the token in Firestore under the user's document
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    db.collection("students")
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



    private fun setupNavigationItemSelection() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    drawerLayout.closeDrawers()
                }
                R.id.nav_view_modules -> {
                    val viewModulesFragment = ViewModulesFragment()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, viewModulesFragment)
                        .addToBackStack(null)
                        .commit()
                    drawerLayout.closeDrawers()
                }
                R.id.nav_profile -> {
                    val profileFragment = ProfileFragment()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, profileFragment)
                        .addToBackStack(null)
                        .commit()
                    drawerLayout.closeDrawers()
                }
                R.id.nav_settings -> {
                    val settingsFragment = SettingsFragment()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, settingsFragment)
                        .addToBackStack(null)
                        .commit()
                    drawerLayout.closeDrawers()
                }
                R.id.nav_logout -> {
                    performLogout()
                }
            }
            true
        }
    }

    private fun setupCampusQueryCards() {
        val campusQueryCards = listOf(
            R.id.lecturerQueryCard to "Lecturer Query",
            R.id.resultsQueryCard to "Results Query",
            R.id.timetableQueryCard to "Timetable Query",
            R.id.financeQueryCard to "Finance Query",
            R.id.assessmentQueryCard to "Assessment Query",
            R.id.studentSystemsQueryCard to "Student Systems Query",
            R.id.pasQueryCard to "Programme Assessment Schedule (PAS) Query",
            R.id.visaQueryCard to "Student Visa / Study Visa Letter / Letter of Conduct"
        )

        campusQueryCards.forEach { (cardId, cardText) ->
            val cardView = view?.findViewById<CardView>(cardId)
            cardView?.setOnClickListener {
                val fragment = when (cardId) {
                    R.id.lecturerQueryCard -> LecturerQueryFragment()
                    R.id.resultsQueryCard -> ResultsQueryFragment()
                    R.id.timetableQueryCard -> TimetableQueryFragment()
                    R.id.financeQueryCard -> FinanceQueryFragment()
                    R.id.assessmentQueryCard -> AssessmentQueryFragment()
                    R.id.studentSystemsQueryCard -> StudentSystemsQueryFragment()
                    R.id.pasQueryCard -> PasQueryFragment()
                    R.id.visaQueryCard -> VisaQueryFragment()
                    else -> StudentDashboardFragment()
                }

                fragment.arguments = Bundle().apply {
                    putString("queryTitle", cardText)
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun setupStudentHubQueryCards() {
        val studentHubQueryCards = listOf(
            Triple(R.id.discontinuationAssessmentCard, R.id.discontinuationAssessmentText, "Application for a Discontinuation Assessment"),
            Triple(R.id.deansExamCard, R.id.deansExamText, "Application for Dean's exam"),
            Triple(R.id.assessmentAppealsCard, R.id.assessmentAppealsText, "Assessment Appeals"),
            Triple(R.id.deferredAssessmentCard, R.id.deferredAssessmentText, "Deferred assessment"),
            Triple(R.id.disciplinaryAppealsCard, R.id.disciplinaryAppealsText, "Disciplinary Appeals"),
            Triple(R.id.externalCreditCard, R.id.externalCreditText, "External Credit"),
            Triple(R.id.increasedCreditLoadCard, R.id.increasedCreditLoadText, "Increased Credit Load"),
            Triple(R.id.moduleExemptionCard, R.id.moduleExemptionText, "Module Exemption")
        )

        studentHubQueryCards.forEach { (cardId, _, cardText) ->
            val cardView = view?.findViewById<CardView>(cardId)
            cardView?.setOnClickListener {
                val fragment = when (cardId) {
                    R.id.discontinuationAssessmentCard -> DiscontinuationAssessmentFragment()
                    R.id.deansExamCard -> DeansExamFragment()
                    R.id.assessmentAppealsCard -> AssessmentAppealsFragment()
                    R.id.deferredAssessmentCard -> DeferredAssessmentFragment()
                    R.id.disciplinaryAppealsCard -> DisciplinaryAppealsFragment()
                    R.id.externalCreditCard -> ExternalCreditFragment()
                    R.id.increasedCreditLoadCard -> IncreasedCreditLoadFragment()
                    R.id.moduleExemptionCard -> ModuleExemptionFragment()
                    else -> StudentDashboardFragment()
                }
                fragment.arguments = Bundle().apply {
                    putString("queryTitle", cardText)
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun setupAlumniQueryCards() {
        val alumniQueryCards = listOf(
            Triple(R.id.applicationDiscontinuationCard, R.id.applicationDiscontinuationText, "Application for a Discontinuation Assessment"),
            Triple(R.id.applicationDeansExamCard, R.id.applicationDeansExamText, "Application for Dean's exam"),
            Triple(R.id.graduationConfirmationCard, R.id.graduationConfirmationText, "Application for Graduation confirmation or Syllabus Request"),
            Triple(R.id.certificateReprintCard, R.id.certificateReprintText, "Request for Certificate Reprint"),
            Triple(R.id.uncollectedCertificatesCard, R.id.uncollectedCertificatesText, "Request for Uncollected Certificates"),
            Triple(R.id.transcriptRequestCard, R.id.transcriptRequestText, "Transcript Request")
        )

        alumniQueryCards.forEach { (cardId, _, cardText) ->
            val cardView = view?.findViewById<CardView>(cardId)
            cardView?.setOnClickListener {
                val fragment = when (cardId) {
                    R.id.applicationDiscontinuationCard -> AlumniDiscontinuationFragment()
                    R.id.applicationDeansExamCard -> AlumniDeansExamFragment()
                    R.id.graduationConfirmationCard -> GraduationConfirmationFragment()
                    R.id.certificateReprintCard -> CertificateReprintFragment()
                    R.id.uncollectedCertificatesCard -> UncollectedCertificatesFragment()
                    R.id.transcriptRequestCard -> TranscriptRequestFragment()
                    else -> StudentDashboardFragment()
                }
                fragment.arguments = Bundle().apply {
                    putString("queryTitle", cardText)
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        // Update UI with current language
        context?.let { ctx ->
            val languageCode = LocaleHelper.getLanguage(ctx)
            LocaleHelper.updateResources(this, languageCode)

            // Refresh views
            navigationView.invalidate()
            drawerLayout.invalidate()
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterCards(newText)
                return true
            }
        })
    }

    private fun filterGridLayout(gridLayout: GridLayout, lowercaseQuery: String) {
        for (i in 0 until gridLayout.childCount) {
            val card = gridLayout.getChildAt(i) as? CardView
            card?.let { cardView ->
                val textView = cardView.findViewById<TextView>(getTextViewIdForCard(cardView.id))
                val cardText = textView?.text?.toString()?.toLowerCase(Locale.getDefault()) ?: ""
                cardView.visibility = if (cardText.contains(lowercaseQuery)) View.VISIBLE else View.GONE
            }
        }
    }

    private fun filterCards(query: String?) {
        query?.let {
            val lowercaseQuery = it.toLowerCase(Locale.getDefault())
            filterGridLayout(gridLayout, lowercaseQuery)
            filterGridLayout(gridLayout1, lowercaseQuery)
            filterGridLayout(alumniQueriesGridLayout, lowercaseQuery)
        }
    }

    private fun getTextViewIdForCard(cardId: Int): Int {
        return when (cardId) {
            // Campus Query cards
            R.id.lecturerQueryCard -> R.id.lecturerQueryText
            R.id.resultsQueryCard -> R.id.resultsQueryText
            R.id.timetableQueryCard -> R.id.timetableQueryText
            R.id.financeQueryCard -> R.id.financeQueryText
            R.id.assessmentQueryCard -> R.id.assessmentQueryText
            R.id.studentSystemsQueryCard -> R.id.studentSystemsQueryText
            R.id.pasQueryCard -> R.id.pasQueryText
            R.id.visaQueryCard -> R.id.visaQueryText

            // Student Hub Query cards
            R.id.discontinuationAssessmentCard -> R.id.discontinuationAssessmentText
            R.id.deansExamCard -> R.id.deansExamText
            R.id.assessmentAppealsCard -> R.id.assessmentAppealsText
            R.id.deferredAssessmentCard -> R.id.deferredAssessmentText
            R.id.disciplinaryAppealsCard -> R.id.disciplinaryAppealsText
            R.id.externalCreditCard -> R.id.externalCreditText
            R.id.increasedCreditLoadCard -> R.id.increasedCreditLoadText
            R.id.moduleExemptionCard -> R.id.moduleExemptionText

            //Alumni Former Students
            R.id.applicationDiscontinuationCard -> R.id.applicationDiscontinuationText
            R.id.applicationDeansExamCard -> R.id.applicationDeansExamText
            R.id.graduationConfirmationCard -> R.id.graduationConfirmationText
            R.id.certificateReprintCard -> R.id.certificateReprintText
            R.id.uncollectedCertificatesCard -> R.id.uncollectedCertificatesText
            R.id.transcriptRequestCard -> R.id.transcriptRequestText

            else -> throw IllegalArgumentException("Unknown card ID: $cardId")
        }
    }




    private fun fetchStudentInfoAndUpdateHeader() {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val email = sharedPref?.getString("studentEmail", "") ?: ""

        if (email.isNotEmpty()) {
            db.collection("students")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val student = documents.documents[0]
                        updateNavigationHeader(
                            student.getString("firstName") ?: "",
                            student.getString("lastName") ?: "",
                            email
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Error fetching student info: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun updateNavigationHeader(firstName: String, lastName: String, email: String) {
        val headerView = navigationView.getHeaderView(0)
        val nameTextView = headerView.findViewById<TextView>(R.id.nav_header_name)
        val emailTextView = headerView.findViewById<TextView>(R.id.nav_header_email)
        val headerImageView = headerView.findViewById<CircleImageView>(R.id.nav_header_image)

        nameTextView.text = "$firstName $lastName"
        emailTextView.text = email

        // Load profile image
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val studentId = sharedPref?.getString("studentId", "") ?: ""

        if (studentId.isNotEmpty()) {
            db.collection("StudentProfile")
                .document(studentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Load and display profile image
                        document.getString("profileImage")?.let { base64Image ->
                            try {
                                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                headerImageView.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                Log.e("ProfileImage", "Error displaying nav header image: ${e.message}")
                                // Set default image if there's an error
                                headerImageView.setImageResource(R.mipmap.ic_launcher_round)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileImage", "Error loading nav header image: ${e.message}")
                    // Set default image if there's an error
                    headerImageView.setImageResource(R.mipmap.ic_launcher_round)
                }
        }
    }


    private fun performLogout() {
        try {
            // Sign out from Firebase Auth
            auth.signOut()

            // Stop the query listener service
            activity?.stopService(Intent(activity, NotificationService::class.java))

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