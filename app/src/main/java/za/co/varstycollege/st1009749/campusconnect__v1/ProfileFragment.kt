package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {
    private lateinit var profileImage: CircleImageView
    private lateinit var firstNameInput: TextInputEditText
    private lateinit var middleNameInput: TextInputEditText
    private lateinit var lastNameInput: TextInputEditText
    private lateinit var genderSpinner: AutoCompleteTextView
    private lateinit var pronounsSpinner: AutoCompleteTextView
    private lateinit var qualificationInput: TextInputEditText
    private lateinit var modulesRecyclerView: RecyclerView
    private lateinit var queriesSubmittedText: TextView
    private lateinit var saveButton: Button
    private lateinit var changePhotoButton: ImageView
    private lateinit var profileImageManager: ProfileImageManager

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentBase64Image: String? = null

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        profileImageManager = ProfileImageManager(requireContext())

        initializeViews(view)
        setupSpinners()
        setupRecyclerView()
        loadUserData()

        // Setup image picker with clearing current image
        changePhotoButton.setOnClickListener {
            // Clear current image
            currentBase64Image = null
            profileImage.setImageResource(R.mipmap.ic_launcher_round)

            // Clear image in Firestore
            val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", Activity.MODE_PRIVATE)
            val studentId = sharedPref?.getString("studentId", "") ?: ""
            if (studentId.isNotEmpty()) {
                db.collection("StudentProfile")
                    .document(studentId)
                    .update("profileImage", null)
                    .addOnSuccessListener {
                        // Open image picker
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(intent, PICK_IMAGE_REQUEST)
                    }
            }
        }

        // Setup save button
        saveButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun initializeViews(view: View) {
        profileImage = view.findViewById(R.id.profileImage)
        firstNameInput = view.findViewById(R.id.firstNameInput)
        middleNameInput = view.findViewById(R.id.middleNameInput)
        lastNameInput = view.findViewById(R.id.lastNameInput)
        genderSpinner = view.findViewById(R.id.genderSpinner)
        pronounsSpinner = view.findViewById(R.id.pronounsSpinner)
        qualificationInput = view.findViewById(R.id.qualificationInput)
        modulesRecyclerView = view.findViewById(R.id.modulesRecyclerView)
        queriesSubmittedText = view.findViewById(R.id.queriesSubmittedText)
        saveButton = view.findViewById(R.id.saveButton)
        changePhotoButton = view.findViewById(R.id.changePhotoButton)
    }

    private fun setupSpinners() {
        val genders = arrayOf("Male", "Female", "Other", "Prefer not to say")
        val genderAdapter = ArrayAdapter(requireContext(), R.layout.list_item, genders)
        genderSpinner.setAdapter(genderAdapter)

        val pronouns = arrayOf("He/Him", "She/Her", "They/Them", "Other", "Prefer not to say")
        val pronounsAdapter = ArrayAdapter(requireContext(), R.layout.list_item, pronouns)
        pronounsSpinner.setAdapter(pronounsAdapter)
    }

    private fun setupRecyclerView() {
        modulesRecyclerView.layoutManager = LinearLayoutManager(context)
        modulesRecyclerView.adapter = ModuleAdapter(emptyList())
    }

    private fun loadUserData() {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", Activity.MODE_PRIVATE)
        val studentId = sharedPref?.getString("studentId", "") ?: ""

        if (studentId.isNotEmpty()) {
            // Load student modules and info
            db.collection("StudentModules").document(studentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Load basic info
                        document.getString("name")?.let { firstNameInput.setText(it) }
                        document.getString("surname")?.let { lastNameInput.setText(it) }
                        document.getString("qualification")?.let { qualificationInput.setText(it) }

                        // Load modules
                        val modulesList = mutableListOf<Module>()
                        val modulesData = document.get("modules") as? List<Map<String, Any>>
                        modulesData?.forEach { moduleMap ->
                            val code = moduleMap["code"] as? String ?: ""
                            val name = moduleMap["name"] as? String ?: ""
                            modulesList.add(Module(code, name))
                        }
                        (modulesRecyclerView.adapter as ModuleAdapter).updateModules(modulesList)
                    }
                }

            // Load profile data including image
            db.collection("StudentProfile")
                .document(studentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        document.getString("middleName")?.let { middleNameInput.setText(it) }
                        document.getString("gender")?.let { genderSpinner.setText(it, false) }
                        document.getString("pronouns")?.let { pronounsSpinner.setText(it, false) }

                        // Load and display profile image
                        document.getString("profileImage")?.let { base64Image ->
                            currentBase64Image = base64Image
                            displayBase64Image(base64Image)
                        }
                    }
                }

            // Count total queries
            db.collection("queries")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener { documents ->
                    queriesSubmittedText.text = "Queries Submitted: ${documents.size()}"
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            profileImageManager.updateProfileImage(
                fragment = this,
                imageUri = data.data!!,
                profileImageView = profileImage
            ) { base64Image ->
                currentBase64Image = base64Image
                saveProfileImageToFirestore(base64Image)
            }
        }
    }

    private fun saveProfile() {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", Activity.MODE_PRIVATE)
        val studentId = sharedPref?.getString("studentId", "") ?: ""

        if (studentId.isEmpty()) {
            Toast.makeText(context, "Error: Student ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val profileData = hashMapOf(
            "middleName" to middleNameInput.text.toString(),
            "gender" to genderSpinner.text.toString(),
            "pronouns" to pronounsSpinner.text.toString()
        )

        // Add profile image if available
        currentBase64Image?.let {
            profileData["profileImage"] = it
        }

        // Save profile data to Firestore
        db.collection("StudentProfile")
            .document(studentId)
            .set(profileData)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfileImageToFirestore(base64Image: String) {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", Activity.MODE_PRIVATE)
        val studentId = sharedPref?.getString("studentId", "") ?: ""

        if (studentId.isNotEmpty()) {
            db.collection("StudentProfile")
                .document(studentId)
                .update("profileImage", base64Image)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileImage", "Error saving to Firestore: ${e.message}")
                }
        }
    }

    private fun displayBase64Image(base64Image: String) {
        try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            profileImage.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("ProfileImage", "Error displaying image: ${e.message}")
        }
    }

    // Module data class and adapter
    data class Module(val code: String, val name: String)

    private inner class ModuleAdapter(private var modules: List<Module>) :
        RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

        inner class ModuleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val codeText: TextView = view.findViewById(R.id.moduleCodeTextView)
            val nameText: TextView = view.findViewById(R.id.moduleNameTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_module, parent, false)
            return ModuleViewHolder(view)
        }

        override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
            val module = modules[position]
            holder.codeText.text = module.code
            holder.nameText.text = module.name
        }

        override fun getItemCount() = modules.size

        fun updateModules(newModules: List<Module>) {
            modules = newModules
            notifyDataSetChanged()
        }
    }
}