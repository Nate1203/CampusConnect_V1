package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminProfileFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var ratingText: TextView
    private lateinit var totalQueriesText: TextView
    private lateinit var averageResponseTimeText: TextView
    private lateinit var profileImage: ImageView
    private lateinit var editProfileButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        nameText = view.findViewById(R.id.adminNameText)
        emailText = view.findViewById(R.id.adminEmailText)
        ratingText = view.findViewById(R.id.adminRatingText)
        totalQueriesText = view.findViewById(R.id.totalQueriesText)
        averageResponseTimeText = view.findViewById(R.id.averageResponseTimeText)
        profileImage = view.findViewById(R.id.adminProfileImage)
        editProfileButton = view.findViewById(R.id.editProfileButton)

        // Load admin data
        loadAdminProfile()

        // Setup edit button
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun loadAdminProfile() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("admins")
                .whereEqualTo("email", user.email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val admin = documents.documents[0]
                        nameText.text = "${admin.getString("firstName")} ${admin.getString("lastName")}"
                        emailText.text = admin.getString("email")

                        // Load statistics
                        loadAdminStatistics(user.email!!)
                    }
                }
        }
    }

    private fun loadAdminStatistics(adminEmail: String) {
        // Load total solved queries
        db.collection("SolvedQueries")
            .whereEqualTo("adminEmail", adminEmail)
            .get()
            .addOnSuccessListener { documents ->
                totalQueriesText.text = "Total Queries Solved: ${documents.size()}"

                // Calculate average response time
                var totalResponseTime = 0L
                documents.forEach { doc ->
                    val submittedDate = doc.getTimestamp("submittedDate")?.toDate()
                    val solvedDate = doc.getTimestamp("solvedDate")?.toDate()
                    if (submittedDate != null && solvedDate != null) {
                        totalResponseTime += solvedDate.time - submittedDate.time
                    }
                }
                if (documents.size() > 0) {
                    val averageTimeHours = (totalResponseTime / documents.size()) / (1000 * 60 * 60)
                    averageResponseTimeText.text = "Average Response Time: ${averageTimeHours}h"
                }
            }

        // Load average rating
        db.collection("AdminReviews")
            .whereEqualTo("adminEmail", adminEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    var totalRating = 0f
                    documents.forEach { doc ->
                        totalRating += doc.getDouble("rating")?.toFloat() ?: 0f
                    }
                    val averageRating = totalRating / documents.size()
                    ratingText.text = String.format("%.1f ★", averageRating)
                } else {
                    ratingText.text = "No ratings yet"
                }
            }
    }

    private fun showEditProfileDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_profile)

        val firstNameEdit = dialog.findViewById<EditText>(R.id.firstNameEdit)
        val lastNameEdit = dialog.findViewById<EditText>(R.id.lastNameEdit)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)

        // Load current data
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("admins")
                .whereEqualTo("email", user.email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val admin = documents.documents[0]
                        firstNameEdit.setText(admin.getString("firstName"))
                        lastNameEdit.setText(admin.getString("lastName"))
                    }
                }
        }

        saveButton.setOnClickListener {
            val firstName = firstNameEdit.text.toString()
            val lastName = lastNameEdit.text.toString()

            currentUser?.let { user ->
                db.collection("admins")
                    .whereEqualTo("email", user.email)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            documents.documents[0].reference.update(mapOf(
                                "firstName" to firstName,
                                "lastName" to lastName
                            )).addOnSuccessListener {
                                loadAdminProfile()
                                dialog.dismiss()
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
        }

        dialog.show()
    }
}