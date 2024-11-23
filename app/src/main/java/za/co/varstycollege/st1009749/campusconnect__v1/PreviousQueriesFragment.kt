package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import android.widget.LinearLayout
import java.util.*

class PreviousQueriesFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var filterButton: FloatingActionButton
    private lateinit var db: FirebaseFirestore
    private val queries = mutableListOf<QueryItem>()
    private lateinit var adapter: QueriesAdapter

    // Filter states
    private var currentTab = "Pending"
    private var selectedCategory = "All Categories"
    private var sortByNewest = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_previous_queries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.queriesRecyclerView)
        tabLayout = view.findViewById(R.id.tabLayout)
        filterButton = view.findViewById(R.id.filterButton)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = QueriesAdapter()
        recyclerView.adapter = adapter

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Setup tab selection listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = when(tab?.position) {
                    0 -> "Pending"
                    1 -> "Processing"
                    2 -> "Solved"
                    else -> "Pending"
                }
                fetchQueries()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Setup filter button
        filterButton.setOnClickListener {
            showFilterDialog()
        }

        // Initial query fetch
        fetchQueries()
    }

    private fun showFilterDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_filter_menu)

        // Initialize dialog views
        val categoryGroup = dialog.findViewById<RadioGroup>(R.id.categoryGroup)
        val sortGroup = dialog.findViewById<RadioGroup>(R.id.sortGroup)
        val resetButton = dialog.findViewById<Button>(R.id.resetButton)
        val applyButton = dialog.findViewById<Button>(R.id.applyButton)

        // Set current selections
        when (selectedCategory) {
            "Campus Queries" -> categoryGroup.check(R.id.campusQueries)
            "Student Hub Queries" -> categoryGroup.check(R.id.studentHubQueries)
            "Alumni Student Queries" -> categoryGroup.check(R.id.alumniQueries)
            else -> categoryGroup.check(R.id.allCategories)
        }

        if (sortByNewest) {
            sortGroup.check(R.id.newest)
        } else {
            sortGroup.check(R.id.oldest)
        }

        // Handle reset
        resetButton.setOnClickListener {
            selectedCategory = "All Categories"
            sortByNewest = true
            fetchQueries()
            dialog.dismiss()
        }

        // Handle apply
        applyButton.setOnClickListener {
            // Get category selection
            selectedCategory = when (categoryGroup.checkedRadioButtonId) {
                R.id.campusQueries -> "Campus Queries"
                R.id.studentHubQueries -> "Student Hub Queries"
                R.id.alumniQueries -> "Alumni Student Queries"
                else -> "All Categories"
            }

            // Get sort selection
            sortByNewest = sortGroup.checkedRadioButtonId == R.id.newest

            fetchQueries()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun fetchQueries() {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val studentId = sharedPref?.getString("studentId", "") ?: ""

        Log.d("PreviousQueries", "Fetching queries for studentId: $studentId with status: $currentTab")

        if (studentId.isNotEmpty()) {
            queries.clear()

            when (currentTab) {
                "Solved" -> fetchSolvedQueries(studentId)
                else -> {
                    when (selectedCategory) {
                        "Alumni Student Queries" -> fetchFromCollection("AlumniQuery", "Alumni Student Queries", studentId)
                        "Campus Queries" -> fetchFromCollection("CampusQuery", "Campus Queries", studentId)
                        "Student Hub Queries" -> fetchFromCollection("StudentHubQuery", "Student Hub Queries", studentId)
                        "All Categories" -> {
                            fetchFromCollection("AlumniQuery", "Alumni Student Queries", studentId)
                            fetchFromCollection("CampusQuery", "Campus Queries", studentId)
                            fetchFromCollection("StudentHubQuery", "Student Hub Queries", studentId)
                        }
                    }
                }
            }
        } else {
            Toast.makeText(context, "Error: Student ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFromCollection(collectionName: String, category: String, studentId: String) {
        db.collection(collectionName)
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    try {
                        val status = document.getString("status") ?: "Pending"
                        Log.d("PreviousQueries", "Document status from $collectionName: $status")

                        val statusMatches = when (currentTab) {
                            "Pending" -> status == "Pending"
                            "Processing" -> status == "Processing"
                            else -> false
                        }

                        if (statusMatches &&
                            (selectedCategory == "All Categories" || selectedCategory == category)) {
                            val query = QueryItem(
                                name = document.getString("queryName") ?: "",
                                type = document.getString("queryType") ?: "",
                                category = category,
                                description = document.getString("description") ?: "",
                                dateSubmitted = document.getTimestamp("dateSubmitted")?.toDate() ?: Date(),
                                status = status
                            )
                            queries.add(query)
                            Log.d("PreviousQueries", "Added $category query with status: ${query.status}")
                        }
                    } catch (e: Exception) {
                        Log.e("PreviousQueries", "Error parsing document from $collectionName: ${e.message}")
                    }
                }
                updateQueryList()
            }
            .addOnFailureListener { e ->
                Log.e("PreviousQueries", "Error fetching from $collectionName: ${e.message}")
            }
    }


    private fun fetchSolvedQueries(studentId: String) {
        db.collection("SolvedQueries")
            .whereEqualTo("studentNumber", studentId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    try {
                        if (selectedCategory == "All Categories" ||
                            selectedCategory == document.getString("queryCategory")) {
                            val query = QueryItem(
                                name = document.getString("queryName") ?: "",
                                type = "Solved Query",
                                category = document.getString("queryCategory") ?: "",
                                description = document.getString("description") ?: "",
                                dateSubmitted = document.getTimestamp("dateSolved")?.toDate() ?: Date(),
                                status = "Solved",
                                adminFeedback = document.getString("adminFeedback"),
                                studentName = document.getString("studentName"),
                                studentNumber = document.getString("studentNumber"),
                                adminEmail = document.getString("adminEmail"), // Add this line
                                adminFirstName = document.getString("adminFirstName"), // Add this line
                                adminLastName = document.getString("adminLastName")  // Add this line
                            )
                            queries.add(query)
                            Log.d("PreviousQueries", "Added solved query: ${query.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("PreviousQueries", "Error parsing solved query document: ${e.message}")
                    }
                }
                updateQueryList()
            }
            .addOnFailureListener { e ->
                Log.e("PreviousQueries", "Error fetching solved queries: ${e.message}")
            }
    }


    private fun updateQueryList() {
        if (sortByNewest) {
            queries.sortByDescending { it.dateSubmitted }
        } else {
            queries.sortBy { it.dateSubmitted }
        }
        adapter.notifyDataSetChanged()

        if (queries.isEmpty()) {
            Toast.makeText(context, "No queries found for selected filters", Toast.LENGTH_SHORT).show()
        }
    }

    inner class QueriesAdapter : RecyclerView.Adapter<QueriesAdapter.QueryViewHolder>() {

        inner class QueryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.queryNameText)
            val typeText: TextView = view.findViewById(R.id.queryTypeText)
            val categoryText: TextView = view.findViewById(R.id.queryCategoryText)
            val dateText: TextView = view.findViewById(R.id.dateSubmittedText)
            val descriptionText: TextView = view.findViewById(R.id.descriptionText)
            val categoryIndicator: View = view.findViewById(R.id.categoryIndicator)
            val statusText: TextView = view.findViewById(R.id.statusText)
            val adminFeedbackText: TextView = view.findViewById(R.id.adminFeedbackText)
            val studentInfoText: TextView = view.findViewById(R.id.studentInfoText)
            val rateServiceButton: Button = view.findViewById(R.id.rateServiceButton)
            val chatButton: ImageButton = view.findViewById(R.id.chatButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_previous_query, parent, false)
            return QueryViewHolder(view)
        }

        override fun onBindViewHolder(holder: QueryViewHolder, position: Int) {
            val query = queries[position]
            holder.nameText.text = query.name
            holder.typeText.text = query.type
            holder.categoryText.text = query.category
            holder.descriptionText.text = query.description
            holder.statusText.text = query.status

            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

            // Hide the extra fields by default
            holder.adminFeedbackText.visibility = View.GONE
            holder.studentInfoText.visibility = View.GONE
            holder.rateServiceButton.visibility = View.GONE
            holder.chatButton.visibility = View.GONE  // Add this line

            // Show extra fields only for solved queries
            if (currentTab == "Solved") {
                holder.dateText.text = "Solved on: ${dateFormat.format(query.dateSubmitted)}"

                if (query.adminFeedback?.isNotEmpty() == true) {
                    holder.adminFeedbackText.visibility = View.VISIBLE
                    holder.adminFeedbackText.text = "Admin Response: ${query.adminFeedback}"
                }

                if (query.studentName?.isNotEmpty() == true && query.studentNumber?.isNotEmpty() == true) {
                    holder.studentInfoText.visibility = View.VISIBLE
                    holder.studentInfoText.text = "${query.studentName} (${query.studentNumber})"
                }

                // Show chat button for solved queries
                holder.chatButton.visibility = View.VISIBLE
                holder.chatButton.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ChatFragment.newInstance(
                            queryId = query.name,
                            adminId = query.adminEmail ?: "",  // Changed this to use adminEmail
                            studentId = query.studentNumber ?: ""
                        ))
                        .addToBackStack(null)
                        .commit()
                }

                // Check if query has been rated
                checkIfQueryRated(query) { isRated ->
                    holder.rateServiceButton.visibility = View.VISIBLE
                    if (isRated) {
                        holder.rateServiceButton.isEnabled = false
                        holder.rateServiceButton.alpha = 0.5f
                        holder.rateServiceButton.text = "Already Rated"
                        holder.rateServiceButton.setOnClickListener {
                            Toast.makeText(context, "You have already submitted a review for this query", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        holder.rateServiceButton.isEnabled = true
                        holder.rateServiceButton.alpha = 1.0f
                        holder.rateServiceButton.text = "Rate Service"
                        holder.rateServiceButton.setOnClickListener {
                            showRatingDialog(query)
                        }
                    }
                }
            } else {
                holder.dateText.text = "Submitted: ${dateFormat.format(query.dateSubmitted)}"
            }

            // Set category indicator color
            val indicatorColor = when (query.category) {
                "Campus Queries" -> holder.itemView.context.getColor(R.color.electric_blue)
                "Student Hub Queries" -> holder.itemView.context.getColor(R.color.purple_pink)
                "Alumni Student Queries" -> holder.itemView.context.getColor(R.color.darker_purple)
                else -> holder.itemView.context.getColor(R.color.white)
            }
            holder.categoryIndicator.setBackgroundColor(indicatorColor)
        }

        override fun getItemCount() = queries.size
    }

    private fun checkIfQueryRated(query: QueryItem, callback: (Boolean) -> Unit) {
        db.collection("AdminReviews")
            .whereEqualTo("queryName", query.name)
            .whereEqualTo("studentNumber", query.studentNumber)
            .get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty())
            }
            .addOnFailureListener {
                callback(false) // In case of error, allow rating
            }
    }

    private fun showRatingDialog(query: QueryItem) {
        val dialog = Dialog(requireContext(), R.style.RatingDialogTheme)
        dialog.setContentView(R.layout.dialog_rate_service)

        // Initialize views
        val adminNameText = dialog.findViewById<TextView>(R.id.adminNameText)
        val adminEmailText = dialog.findViewById<TextView>(R.id.adminEmailText)
        val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
        val feedbackText = dialog.findViewById<EditText>(R.id.feedbackText)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val submitButton = dialog.findViewById<Button>(R.id.submitButton)

        // Store admin details for later use
        var adminFirstName = ""
        var adminLastName = ""
        var adminEmail = ""

        // Get admin info from Firestore
        db.collection("SolvedQueries")
            .whereEqualTo("queryName", query.name)
            .whereEqualTo("studentNumber", query.studentNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    adminFirstName = doc.getString("adminFirstName") ?: ""
                    adminLastName = doc.getString("adminLastName") ?: ""
                    adminEmail = doc.getString("adminEmail") ?: ""

                    adminNameText.text = "$adminFirstName $adminLastName"
                    adminEmailText.text = adminEmail
                }
            }

        // Handle button clicks
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            val feedback = feedbackText.text.toString().trim()

            // Validate all fields
            when {
                rating == 0f -> {
                    Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                feedback.isEmpty() -> {
                    feedbackText.error = "Please provide feedback"
                    return@setOnClickListener
                }
                adminFirstName.isEmpty() || adminLastName.isEmpty() || adminEmail.isEmpty() -> {
                    Toast.makeText(context, "Error: Admin information not found", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Create review document
            val reviewData = hashMapOf(
                "queryName" to query.name,
                "queryCategory" to query.category,
                "studentNumber" to query.studentNumber,
                "studentName" to query.studentName,
                "adminFirstName" to adminFirstName,
                "adminLastName" to adminLastName,
                "adminEmail" to adminEmail,
                "rating" to rating,
                "feedback" to feedback,
                "dateSubmitted" to Date(),
                "queryDescription" to query.description
            )

            // Save to Firestore under AdminReviews collection
            db.collection("AdminReviews")
                .add(reviewData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Review submitted successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()

                    // Refresh the queries to update the button state
                    fetchQueries()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error submitting review: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }


    data class QueryItem(
        val name: String,
        val type: String,
        val category: String,
        val description: String,
        val dateSubmitted: Date,
        val status: String,
        val adminFeedback: String? = null,
        val studentName: String? = null,
        val studentNumber: String? = null,
        val adminEmail: String? = null,      // Add this
        val adminFirstName: String? = null,  // Add this
        val adminLastName: String? = null    // Add this
    )
}