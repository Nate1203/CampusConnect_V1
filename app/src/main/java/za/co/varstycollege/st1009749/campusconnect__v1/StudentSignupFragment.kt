package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class StudentSignupFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_student_signup, container, false)

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val nameInput = view.findViewById<TextInputEditText>(R.id.nameInput)
        val surnameInput = view.findViewById<TextInputEditText>(R.id.surnameInput)
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.passwordInput)
        val confirmPasswordInput = view.findViewById<TextInputEditText>(R.id.confirmPasswordInput)
        val signupButton = view.findViewById<Button>(R.id.signupButton)

        signupButton.setOnClickListener {
            val name = nameInput.text.toString()
            val surname = surnameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (validateInputs(name, surname, email, password, confirmPassword)) {
                createUserWithEmailAndPassword(name, surname, email, password)
            }
        }

        return view
    }

    private fun validateInputs(name: String, surname: String, email: String, password: String, confirmPassword: String): Boolean {
        when {
            name.isEmpty() -> showToast("Name is required")
            surname.isEmpty() -> showToast("Surname is required")
            email.isEmpty() -> showToast("Email is required")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showToast("Invalid email format")
            password.isEmpty() -> showToast("Password is required")
            password.length < 6 -> showToast("Password should be at least 6 characters long")
            password != confirmPassword -> showToast("Passwords do not match")
            else -> return true
        }
        return false
    }

    private fun createUserWithEmailAndPassword(name: String, surname: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    // After successful authentication, save user data to Firestore
                    signUpStudent(name, surname, email, user.uid)
                }
            }
            .addOnFailureListener { e ->
                showToast("Authentication failed: ${e.message}")
            }
    }

    private fun signUpStudent(name: String, surname: String, email: String, uid: String) {
        val studentId = generateStudentId()
        val studentData = hashMapOf(
            "firstName" to name,
            "lastName" to surname,
            "email" to email,
            "studentId" to studentId,
            "uid" to uid,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("students")
            .document(uid)  // Use Firebase Auth UID as document ID
            .set(studentData)
            .addOnSuccessListener {
                showStudentIdDialog(studentId)
            }
            .addOnFailureListener { e ->
                showToast("Error saving student data: ${e.message}")
            }
    }

    private fun generateStudentId(): String {
        return (2400000000 + Random().nextInt(99999999)).toString()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showStudentIdDialog(studentId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Your Student ID")
            .setMessage("Your Student ID is: $studentId\n\nPlease write this down and keep it safe.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                navigateToCampusRegistration(studentId)
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun navigateToCampusRegistration(studentId: String) {
        val campusRegistrationFragment = CampusRegistrationFragment().apply {
            arguments = Bundle().apply {
                putString("studentId", studentId)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, campusRegistrationFragment)
            .addToBackStack(null)
            .commit()
    }
}