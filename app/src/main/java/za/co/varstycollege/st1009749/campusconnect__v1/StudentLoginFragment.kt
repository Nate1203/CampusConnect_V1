package za.co.varstycollege.st1009749.campusconnect__v1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class StudentLoginFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_student_login, container, false)

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        auth.currentUser?.let { user ->
            // User is already signed in, get their data and proceed to dashboard
            fetchUserDataAndNavigate(user.uid)
            return view
        }

        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.passwordInput)
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signInWithEmailPassword(email, password)
            } else {
                showToast("Please enter email and password")
            }
        }

        registerButton.setOnClickListener {
            (activity as? MainActivity)?.showSignupPage(StudentSignupFragment())
        }

        return view
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    // Fetch additional user data from Firestore
                    fetchUserDataAndNavigate(user.uid)
                } else {
                    showToast("Login failed")
                }
            }
            .addOnFailureListener { exception ->
                when (exception) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                        showToast("No account found with this email")
                    }
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                        showToast("Incorrect password")
                    }
                    else -> {
                        showToast("Error during login: ${exception.message}")
                    }
                }
            }
    }

    private fun fetchUserDataAndNavigate(uid: String) {
        db.collection("students")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val email = document.getString("email") ?: ""
                    val studentId = document.getString("studentId") ?: document.id

                    // Store session data
                    storeLoginSession(email, studentId)

                    // Show success message and navigate
                    showToast("Login successful")
                    (activity as? MainActivity)?.showDashboard(StudentDashboardFragment())
                } else {
                    showToast("Error: Student data not found")
                    auth.signOut() // Sign out since data is inconsistent
                }
            }
            .addOnFailureListener { exception ->
                showToast("Error fetching student data: ${exception.message}")
                auth.signOut() // Sign out since we couldn't get the data
            }
    }

    private fun storeLoginSession(email: String, studentId: String) {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        with (sharedPref?.edit()) {
            this?.putString("studentEmail", email)
            this?.putString("studentId", studentId)
            this?.putString("loginToken", UUID.randomUUID().toString())
            this?.apply()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        auth.currentUser?.let { user ->
            fetchUserDataAndNavigate(user.uid)
        }
    }
}