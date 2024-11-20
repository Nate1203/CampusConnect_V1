package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class QueryDetailsFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var queryId: String
    private lateinit var queryCategory: String
    private lateinit var startProcessingButton: Button
    private lateinit var solveQueryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queryId = requireArguments().getString("queryId", "")
        queryCategory = requireArguments().getString("queryCategory", "")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_query_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()

        val queryTitle = view.findViewById<TextView>(R.id.queryTitle)
        val queryType = view.findViewById<TextView>(R.id.queryType)
        val queryCategoryView = view.findViewById<TextView>(R.id.queryCategory)
        val dateSubmitted = view.findViewById<TextView>(R.id.dateSubmitted)
        val queryDescription = view.findViewById<TextView>(R.id.queryDescription)
        startProcessingButton = view.findViewById(R.id.startProcessingButton)
        solveQueryButton = view.findViewById(R.id.solveQueryButton)

        val collectionName = when {
            queryCategory.contains("Campus", ignoreCase = true) -> "CampusQuery"
            queryCategory.contains("Student Hub", ignoreCase = true) -> "StudentHubQuery"
            queryCategory.contains("Alumni", ignoreCase = true) -> "AlumniQuery"
            else -> "CampusQuery"
        }

        db.collection(collectionName).document(queryId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    queryTitle.text = document.getString("queryName")
                    queryType.text = "Type: ${document.getString("queryType")}"
                    queryCategoryView.text = "Category: $queryCategory"

                    val timestamp = document.getTimestamp("dateSubmitted")
                    val dateString = if (timestamp != null) {
                        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm:ss", Locale.ENGLISH)
                        dateFormat.format(timestamp.toDate())
                    } else {
                        "Date not available"
                    }
                    dateSubmitted.text = "Submitted: $dateString"

                    queryDescription.text = document.getString("description")

                    when (document.getString("status")) {
                        "Processing" -> {
                            startProcessingButton.visibility = View.GONE
                            solveQueryButton.visibility = View.VISIBLE
                        }
                        "Pending" -> {
                            startProcessingButton.visibility = View.VISIBLE
                            solveQueryButton.visibility = View.GONE
                        }
                        else -> {
                            startProcessingButton.visibility = View.GONE
                            solveQueryButton.visibility = View.GONE
                        }
                    }
                }
            }

        startProcessingButton.setOnClickListener {
            updateQueryStatus(collectionName)
        }

        solveQueryButton.setOnClickListener {
            val completeFragment = CompleteQueryFragment.newInstance(
                queryId,
                queryType.text.toString().replace("Type: ", ""),
                queryCategory
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, completeFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateQueryStatus(collectionName: String) {
        db.collection(collectionName).document(queryId)
            .update("status", "Processing")
            .addOnSuccessListener {
                Toast.makeText(context, "Query status updated to Processing", Toast.LENGTH_SHORT).show()
                startProcessingButton.visibility = View.GONE
                solveQueryButton.visibility = View.VISIBLE
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        fun newInstance(queryId: String, queryCategory: String): QueryDetailsFragment {
            return QueryDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString("queryId", queryId)
                    putString("queryCategory", queryCategory)
                }
            }
        }
    }
}