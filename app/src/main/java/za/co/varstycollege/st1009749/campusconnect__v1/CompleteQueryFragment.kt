package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.view.Gravity
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp

class CompleteQueryFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var queryId: String
    private lateinit var queryType: String
    private lateinit var queryCategory: String
    private lateinit var contentLayout: LinearLayout
    private lateinit var feedbackInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queryId = requireArguments().getString("queryId", "")
        queryType = requireArguments().getString("queryType", "")
        queryCategory = requireArguments().getString("queryCategory", "")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_complete_query, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()
        contentLayout = view.findViewById(R.id.queryContentLayout)

        val collectionName = when {
            queryCategory.contains("Campus", ignoreCase = true) -> "CampusQuery"
            queryCategory.contains("Student Hub", ignoreCase = true) -> "StudentHubQuery"
            queryCategory.contains("Alumni", ignoreCase = true) -> "AlumniQuery"
            else -> "CampusQuery"
        }

        db.collection(collectionName).document(queryId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    displayQueryFields(document.data)
                }
            }
    }

    private fun displayQueryFields(data: Map<String, Any>?) {
        if (data == null) return

        contentLayout.removeAllViews()
        addHeading("Query Details")

        val commonFields = listOf("queryName", "queryType", "queryCategory", "dateSubmitted", "status")
        commonFields.forEach { field ->
            if (data.containsKey(field)) {
                val value = when (field) {
                    "dateSubmitted" -> {
                        val timestamp = data[field] as com.google.firebase.Timestamp
                        val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm:ss", java.util.Locale.ENGLISH)
                        dateFormat.format(timestamp.toDate())
                    }
                    else -> data[field].toString()
                }
                addField(formatFieldName(field), value)
            }
        }

        addSpacerLine()
        addHeading("Query Specific Information")

        data.forEach { (key, value) ->
            if (!commonFields.contains(key) && key != "description") {
                addField(formatFieldName(key), value.toString())
            }
        }

        data["description"]?.let { description ->
            addSpacerLine()
            addHeading("Query Description")
            addDescription(description.toString())
        }

        addSpacerLine()
        addHeading("Admin Feedback")
        addFeedbackInput()
        addSolveButton()
    }

    private fun addHeading(text: String) {
        val textView = TextView(context).apply {
            this.text = text
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
                bottomMargin = 16
            }
        }
        contentLayout.addView(textView)
    }

    private fun addField(fieldName: String, value: String) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val nameView = TextView(context).apply {
            text = fieldName
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.electric_blue))
        }
        layout.addView(nameView)

        val valueView = TextView(context).apply {
            text = value
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }
        layout.addView(valueView)

        contentLayout.addView(layout)
    }

    private fun addDescription(description: String) {
        val textView = TextView(context).apply {
            this.text = description
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(16, 16, 16, 16)
            background = ContextCompat.getDrawable(context, R.drawable.rounded_background)
        }
        contentLayout.addView(textView)
    }

    private fun addFeedbackInput() {
        feedbackInput = EditText(context).apply {
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setHintTextColor(ContextCompat.getColor(context, android.R.color.white))
            hint = "Enter feedback for the student..."
            minHeight = 150
            gravity = Gravity.TOP
            background = ContextCompat.getDrawable(context, R.drawable.rounded_background)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
                bottomMargin = 24
            }
        }
        contentLayout.addView(feedbackInput)
    }

    private fun addSpacerLine() {
        View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            alpha = 0.1f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                topMargin = 16
                bottomMargin = 16
            }
        }.also { contentLayout.addView(it) }
    }

    private fun addSolveButton() {
        val button = Button(context).apply {
            text = "Mark as Solved"
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.electric_blue)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
            }
            setOnClickListener {
                markQueryAsSolved()
                saveSolvedQuery()
            }
        }
        contentLayout.addView(button)
    }

    private fun formatFieldName(fieldName: String): String {
        return fieldName
            .replace("([A-Z])".toRegex(), " $1")
            .replaceFirstChar { it.uppercase() }
    }

    private fun markQueryAsSolved() {
        val collectionName = when {
            queryCategory.contains("Campus", ignoreCase = true) -> "CampusQuery"
            queryCategory.contains("Student Hub", ignoreCase = true) -> "StudentHubQuery"
            queryCategory.contains("Alumni", ignoreCase = true) -> "AlumniQuery"
            else -> "CampusQuery"
        }

        db.collection(collectionName).document(queryId)
            .update("status", "Solved")
            .addOnSuccessListener {
                Toast.makeText(context, "Query marked as solved", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun saveSolvedQuery() {
        val collectionName = when {
            queryCategory.contains("Campus", ignoreCase = true) -> "CampusQuery"
            queryCategory.contains("Student Hub", ignoreCase = true) -> "StudentHubQuery"
            queryCategory.contains("Alumni", ignoreCase = true) -> "AlumniQuery"
            else -> "CampusQuery"
        }

        // Get admin info from SharedPreferences
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val adminEmail = sharedPref?.getString("adminEmail", "") ?: ""
        val adminFirstName = sharedPref?.getString("adminFirstName", "") ?: ""
        val adminLastName = sharedPref?.getString("adminLastName", "") ?: ""

        db.collection(collectionName).document(queryId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val solvedQuery = hashMapOf(
                        "studentNumber" to (document.getString("studentId") ?: ""),
                        "studentName" to (document.getString("name") ?: "") + " " + (document.getString("surname") ?: ""),
                        "adminFeedback" to feedbackInput.text.toString(),
                        "queryCategory" to queryCategory,
                        "queryName" to (document.getString("queryName") ?: ""),
                        "description" to (document.getString("description") ?: ""),
                        "dateSolved" to Timestamp.now(),
                        "adminFirstName" to adminFirstName,
                        "adminLastName" to adminLastName,
                        "adminEmail" to adminEmail
                    )

                    db.collection("SolvedQueries")
                        .add(solvedQuery)
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error saving to SolvedQueries: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }


    companion object {
        fun newInstance(queryId: String, queryType: String, queryCategory: String): CompleteQueryFragment {
            return CompleteQueryFragment().apply {
                arguments = Bundle().apply {
                    putString("queryId", queryId)
                    putString("queryType", queryType)
                    putString("queryCategory", queryCategory)
                }
            }
        }
    }
}