package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.text.SimpleDateFormat
import com.google.firebase.Timestamp

class SolveQueriesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var pendingTab: TextView
    private lateinit var processingTab: TextView
    private lateinit var tabIndicator: View
    private lateinit var queriesRecyclerView: RecyclerView
    private lateinit var filterButton: FloatingActionButton
    private var currentStatus = "Pending"
    private var selectedCategory = "All Categories"
    private var sortByNewest = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_solve_queries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        initializeViews(view)
        setupTabs()
        setupRecyclerView()
        loadQueries(currentStatus)
    }

    private fun initializeViews(view: View) {
        pendingTab = view.findViewById(R.id.pendingTab)
        processingTab = view.findViewById(R.id.processingTab)
        tabIndicator = view.findViewById(R.id.tabIndicator)
        queriesRecyclerView = view.findViewById(R.id.queriesRecyclerView)
        filterButton = view.findViewById(R.id.filterButton)

        filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupTabs() {
        pendingTab.setOnClickListener {
            currentStatus = "Pending"
            updateTabSelection()
            loadQueries(currentStatus)
        }

        processingTab.setOnClickListener {
            currentStatus = "Processing"
            updateTabSelection()
            loadQueries(currentStatus)
        }
    }


    private fun updateTabSelection() {
        pendingTab.setTextColor(if (currentStatus == "Pending")
            requireContext().getColor(R.color.electric_blue) else Color.WHITE)
        processingTab.setTextColor(if (currentStatus == "Processing")
            requireContext().getColor(R.color.electric_blue) else Color.WHITE)

        tabIndicator.post {
            val tabWidth = pendingTab.width  // Get width of one tab
            val isProcessing = currentStatus == "Processing"

            // Set the width of the indicator to match tab width
            val layoutParams = tabIndicator.layoutParams
            layoutParams.width = tabWidth
            tabIndicator.layoutParams = layoutParams

            // Animate to the correct position
            tabIndicator.animate()
                .translationX(if (isProcessing) tabWidth.toFloat() else 0f)
                .setDuration(200)
                .start()
        }
    }


    private fun setupRecyclerView() {
        queriesRecyclerView.layoutManager = LinearLayoutManager(context)
        queriesRecyclerView.adapter = QueriesAdapter { query ->
            val detailsFragment = QueryDetailsFragment.newInstance(query.id, query.queryCategory)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailsFragment)
                .addToBackStack(null)
                .commit()
        }
    }


    private fun showFilterDialog() {
        val dialog = Dialog(requireContext(), R.style.FilterDialogTheme)
        dialog.setContentView(R.layout.dialog_admin_filter_menu)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val categoryGroup = dialog.findViewById<RadioGroup>(R.id.categoryGroup)
        val sortGroup = dialog.findViewById<RadioGroup>(R.id.sortGroup)
        val resetButton = dialog.findViewById<Button>(R.id.resetButton)

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

        resetButton.setOnClickListener {
            selectedCategory = "All Categories"
            sortByNewest = true
            loadQueries(currentStatus)
            dialog.dismiss()
        }

        categoryGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedCategory = when (checkedId) {
                R.id.campusQueries -> "Campus Queries"
                R.id.studentHubQueries -> "Student Hub Queries"
                R.id.alumniQueries -> "Alumni Student Queries"
                else -> "All Categories"
            }
            loadQueries(currentStatus)
        }

        sortGroup.setOnCheckedChangeListener { _, checkedId ->
            sortByNewest = checkedId == R.id.newest
            loadQueries(currentStatus)
        }

        dialog.show()
    }


    private fun loadQueries(status: String) {
        val queries = mutableListOf<Query>()

        fun fetchFromCollection(collectionName: String, category: String) {
            db.collection(collectionName)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        if (selectedCategory == "All Categories" || selectedCategory == category) {
                            queries.add(createQueryFromDocument(document))
                        }
                    }

                    if (sortByNewest) {
                        queries.sortByDescending { it.dateSubmitted }
                    } else {
                        queries.sortBy { it.dateSubmitted }
                    }

                    updateRecyclerView(queries)
                }
        }

        when (selectedCategory) {
            "Campus Queries" -> fetchFromCollection("CampusQuery", "Campus Queries")
            "Student Hub Queries" -> fetchFromCollection("StudentHubQuery", "Student Hub Queries")
            "Alumni Student Queries" -> fetchFromCollection("AlumniQuery", "Alumni Student Queries")
            "All Categories" -> {
                fetchFromCollection("CampusQuery", "Campus Queries")
                fetchFromCollection("StudentHubQuery", "Student Hub Queries")
                fetchFromCollection("AlumniQuery", "Alumni Student Queries")
            }
        }
    }

    private fun createQueryFromDocument(document: DocumentSnapshot): Query {
        val timestamp = document.getTimestamp("dateSubmitted")
        val dateString = if (timestamp != null) {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm:ss", Locale.ENGLISH)
            dateFormat.format(timestamp.toDate())
        } else {
            "Date not available"
        }

        return Query(
            id = document.id,
            queryName = document.getString("queryName") ?: "",
            queryType = document.getString("queryType") ?: "",
            queryCategory = document.getString("queryCategory") ?: "",
            dateSubmitted = dateString,
            status = document.getString("status") ?: ""
        )
    }

    private fun updateRecyclerView(queries: List<Query>) {
        (queriesRecyclerView.adapter as QueriesAdapter).submitList(queries)
    }

    data class Query(
        val id: String,
        val queryName: String,
        val queryType: String,
        val queryCategory: String,
        val dateSubmitted: String,
        val status: String
    )

    class QueriesAdapter(private val onQueryClick: (Query) -> Unit) :
        ListAdapter<Query, QueriesAdapter.QueryViewHolder>(QueryDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.admin_solved_queries_query_item, parent, false)
            return QueryViewHolder(view, onQueryClick)
        }

        override fun onBindViewHolder(holder: QueryViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class QueryViewHolder(
            itemView: View,
            private val onQueryClick: (Query) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {

            private val leftIndicator: View = itemView.findViewById(R.id.leftIndicator)
            private val queryNameText: TextView = itemView.findViewById(R.id.queryNameText)
            private val queryTypeText: TextView = itemView.findViewById(R.id.queryTypeText)
            private val queryCategoryText: TextView = itemView.findViewById(R.id.queryCategory)
            private val submittedDateText: TextView = itemView.findViewById(R.id.submittedDateText)

            fun bind(query: Query) {
                queryNameText.text = query.queryName
                queryTypeText.text = query.queryType
                queryCategoryText.text = query.queryCategory
                submittedDateText.text = "Submitted: ${query.dateSubmitted}"

                val indicatorColor = when {
                    query.queryCategory.contains("Campus", ignoreCase = true) -> Color.parseColor("#7DF9FF")
                    query.queryCategory.contains("Student Hub", ignoreCase = true) -> Color.parseColor("#e83e8c")
                    query.queryCategory.contains("Alumni", ignoreCase = true) -> Color.parseColor("#6f42c1")
                    else -> Color.parseColor("#7DF9FF")
                }

                leftIndicator.setBackgroundColor(indicatorColor)

                itemView.setOnClickListener {
                    onQueryClick(query)
                }
            }
        }

        class QueryDiffCallback : DiffUtil.ItemCallback<Query>() {
            override fun areItemsTheSame(oldItem: Query, newItem: Query) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Query, newItem: Query) =
                oldItem == newItem
        }
    }
}