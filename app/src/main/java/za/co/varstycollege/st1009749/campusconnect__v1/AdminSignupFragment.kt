package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminSignupFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_admin_signup, container, false)

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        val nameInput = view.findViewById<TextInputEditText>(R.id.adminNameInput)
        val surnameInput = view.findViewById<TextInputEditText>(R.id.adminSurnameInput)
        val emailInput = view.findViewById<TextInputEditText>(R.id.adminEmailInput)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.adminPasswordInput)
        val confirmPasswordInput = view.findViewById<TextInputEditText>(R.id.adminConfirmPasswordInput)
        val signupButton = view.findViewById<Button>(R.id.adminSignupButton)

        // Initialize checkboxes for areas of expertise
        val campusQueryCheckbox = view.findViewById<CheckBox>(R.id.campusQueryCheckbox)
        val studentHubQueryCheckbox = view.findViewById<CheckBox>(R.id.studentHubQueryCheckbox)
        val alumniQueryCheckbox = view.findViewById<CheckBox>(R.id.alumniQueryCheckbox)

        signupButton.setOnClickListener {
            val name = nameInput.text.toString()
            val surname = surnameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            // Collect selected expertise areas
            val expertise = mutableListOf<String>().apply {
                if (campusQueryCheckbox.isChecked) add("CampusQuery")
                if (studentHubQueryCheckbox.isChecked) add("StudentHubQuery")
                if (alumniQueryCheckbox.isChecked) add("AlumniQuery")
            }

            if (validateInputs(name, surname, email, password, confirmPassword, expertise)) {
                createAdminWithEmailAndPassword(name, surname, email, password, expertise)
            }
        }

        return view
    }

    private fun validateInputs(
        name: String,
        surname: String,
        email: String,
        password: String,
        confirmPassword: String,
        expertise: List<String>
    ): Boolean {
        when {
            name.isEmpty() -> showToast("Name is required")
            surname.isEmpty() -> showToast("Surname is required")
            email.isEmpty() -> showToast("Email is required")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showToast("Invalid email format")
            password.isEmpty() -> showToast("Password is required")
            password.length < 6 -> showToast("Password should be at least 6 characters long")
            password != confirmPassword -> showToast("Passwords do not match")
            expertise.isEmpty() -> showToast("Please select at least one area of expertise")
            else -> return true
        }
        return false
    }

    private fun createAdminWithEmailAndPassword(
        name: String,
        surname: String,
        email: String,
        password: String,
        expertise: List<String>
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    saveAdminToFirestore(name, surname, email, user.uid, expertise)
                }
            }
            .addOnFailureListener { e ->
                showToast("Authentication failed: ${e.message}")
            }
    }

    private fun saveAdminToFirestore(
        name: String,
        surname: String,
        email: String,
        uid: String,
        expertise: List<String>
    ) {
        val adminData = hashMapOf(
            "firstName" to name,
            "lastName" to surname,
            "email" to email,
            "uid" to uid,
            "expertise" to expertise,
            "role" to "admin", // This field will help distinguish between admin and student accounts
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("admins")
            .document(uid)
            .set(adminData)
            .addOnSuccessListener {
                showToast("Admin account created successfully")
                // Navigate back to the admin login page
                (activity as? MainActivity)?.showLoginPage(isAdmin = true)
            }
            .addOnFailureListener { e ->
                showToast("Error saving admin data: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}