package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class AdminProfileFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Profile Image Views
    private lateinit var adminProfileImage: CircleImageView
    private lateinit var changePhotoButton: ImageView

    // Personal Info Fields
    private lateinit var firstNameInput: TextInputEditText
    private lateinit var middleNameInput: TextInputEditText
    private lateinit var lastNameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private var genderSpinner: AutoCompleteTextView? = null
    private var pronounsSpinner: AutoCompleteTextView? = null

    // Statistics
    private lateinit var totalQueriesText: TextView
    private lateinit var adminRatingText: TextView
    private lateinit var adminLevelText: TextView
    private lateinit var saveButton: Button

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PROFILE_IMAGE_FILE = "admin_profile_image.jpg"
    }

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
        initializeViews(view)
        setupSpinners()

        // Load admin data
        loadAdminProfile()
        loadLocalProfileImage()

        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        try {
            // Profile Image Views
            adminProfileImage = view.findViewById(R.id.adminProfileImage)
            changePhotoButton = view.findViewById(R.id.changePhotoButton)

            // Personal Info Fields
            firstNameInput = view.findViewById(R.id.firstNameInput)
            middleNameInput = view.findViewById(R.id.middleNameInput)
            lastNameInput = view.findViewById(R.id.lastNameInput)
            emailInput = view.findViewById(R.id.emailInput)
            genderSpinner = view.findViewById(R.id.genderSpinner)
            pronounsSpinner = view.findViewById(R.id.pronounsSpinner)

            // Statistics
            totalQueriesText = view.findViewById(R.id.totalQueriesText)
            adminRatingText = view.findViewById(R.id.adminRatingText)
            adminLevelText = view.findViewById(R.id.adminLevelText)
            saveButton = view.findViewById(R.id.saveButton)

        } catch (e: Exception) {
            Log.e("AdminProfile", "Error initializing views: ${e.message}")
            Toast.makeText(context, "Error initializing profile views", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        try {
            context?.let { ctx ->
                // Setup Gender Spinner
                genderSpinner?.let { spinner ->
                    val genders = arrayOf("Male", "Female", "Non-binary", "Prefer not to say")
                    val genderAdapter = ArrayAdapter(ctx, R.layout.dropdown_item, genders)
                    spinner.setAdapter(genderAdapter)
                }

                // Setup Pronouns Spinner
                pronounsSpinner?.let { spinner ->
                    val pronouns = arrayOf("He/Him", "She/Her", "They/Them", "Prefer not to say")
                    val pronounsAdapter = ArrayAdapter(ctx, R.layout.dropdown_item, pronouns)
                    spinner.setAdapter(pronounsAdapter)
                }
            }
        } catch (e: Exception) {
            Log.e("AdminProfile", "Error setting up spinners: ${e.message}")
        }
    }

    private fun saveProfile() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Get all the values to save
            val middleName = middleNameInput.text.toString()
            val gender = genderSpinner?.text.toString()
            val pronouns = pronounsSpinner?.text.toString()

            // Create profile data map
            val profileData = hashMapOf(
                "userId" to user.uid,
                "email" to user.email,
                "firstName" to firstNameInput.text.toString(),
                "middleName" to middleName,
                "lastName" to lastNameInput.text.toString(),
                "gender" to gender,
                "pronouns" to pronouns,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )

            // Save to AdminProfile collection
            db.collection("AdminProfile")
                .document(user.uid)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadAdminProfile() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            emailInput.setText(user.email)

            // First try to load from AdminProfile collection
            db.collection("AdminProfile")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Load data from AdminProfile
                        firstNameInput.setText(document.getString("firstName"))
                        middleNameInput.setText(document.getString("middleName"))
                        lastNameInput.setText(document.getString("lastName"))
                        genderSpinner?.setText(document.getString("gender") ?: "", false)
                        pronounsSpinner?.setText(document.getString("pronouns") ?: "", false)

                        // Load statistics
                        loadAdminStatistics(user.email!!)
                    } else {
                        // If no AdminProfile document exists, try loading from admins collection
                        db.collection("admins")
                            .whereEqualTo("email", user.email)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val admin = documents.documents[0]
                                    firstNameInput.setText(admin.getString("firstName"))
                                    middleNameInput.setText(admin.getString("middleName"))
                                    lastNameInput.setText(admin.getString("lastName"))
                                    genderSpinner?.setText(admin.getString("gender") ?: "", false)
                                    pronounsSpinner?.setText(admin.getString("pronouns") ?: "", false)

                                    // Create initial AdminProfile document
                                    saveProfile()
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadAdminStatistics(adminEmail: String) {
        // Load total solved queries
        db.collection("SolvedQueries")
            .whereEqualTo("adminEmail", adminEmail)
            .get()
            .addOnSuccessListener { solvedDocs ->
                val totalSolved = solvedDocs.size()
                totalQueriesText.text = "Queries Solved: $totalSolved"

                // Calculate admin level (1 level per 100 XP, 25 XP per query)
                val xp = totalSolved * 25
                val level = (xp / 100) + 1
                adminLevelText.text = "Admin Level: $level"

                // Calculate rating
                db.collection("AdminReviews")
                    .whereEqualTo("adminEmail", adminEmail)
                    .get()
                    .addOnSuccessListener { ratingDocs ->
                        if (!ratingDocs.isEmpty) {
                            var totalRating = 0f
                            ratingDocs.forEach { doc ->
                                totalRating += doc.getDouble("rating")?.toFloat() ?: 0f
                            }
                            val averageRating = totalRating / ratingDocs.size()
                            adminRatingText.text = String.format("Rating: %.1f ★", averageRating)
                        } else {
                            adminRatingText.text = "No Ratings Yet"
                        }
                    }
                    .addOnFailureListener { e ->
                        adminRatingText.text = "Rating: Error loading"
                        Toast.makeText(context, "Error loading ratings: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                totalQueriesText.text = "Queries: Error loading"
                adminLevelText.text = "Level: Error loading"
                Toast.makeText(context, "Error loading statistics: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadLocalProfileImage() {
        val imageFile = File(context?.filesDir, PROFILE_IMAGE_FILE)
        if (imageFile.exists()) {
            // Load into profile image
            Glide.with(this)
                .load(imageFile)
                .placeholder(R.mipmap.ic_launcher_round)
                .into(adminProfileImage)

            // Load into nav header
            val navigationView = activity?.findViewById<NavigationView>(R.id.admin_nav_view)
            val headerView = navigationView?.getHeaderView(0)
            val navHeaderImage = headerView?.findViewById<CircleImageView>(R.id.nav_header_image)

            navHeaderImage?.let { headerImg ->
                Glide.with(this)
                    .load(imageFile)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .into(headerImg)
            }
        }
    }

    private fun saveImageLocally(imageUri: Uri) {
        try {
            val progressDialog = ProgressDialog(context).apply {
                setMessage("Updating profile picture...")
                show()
            }

            val inputStream = context?.contentResolver?.openInputStream(imageUri)
            val outputFile = File(context?.filesDir, PROFILE_IMAGE_FILE)

            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            progressDialog.dismiss()
            loadLocalProfileImage()
            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
            Log.e("Profile", "Error saving image: ${e.message}")
        }
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            saveProfile()
        }

        changePhotoButton.setOnClickListener {
            pickImage()
        }
    }

    private fun pickImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            saveImageLocally(data.data!!)
        }
    }
}
