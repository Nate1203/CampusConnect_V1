package za.co.varstycollege.st1009749.campusconnect__v1

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

class AdminLoginFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_admin_login, container, false)

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        auth.currentUser?.let { user ->
            fetchAdminDataAndNavigate(user.uid)
            return view
        }

        val emailInput = view.findViewById<TextInputEditText>(R.id.adminEmailInput)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.adminPasswordInput)
        val loginButton = view.findViewById<Button>(R.id.adminLoginButton)
        val registerButton = view.findViewById<Button>(R.id.adminRegisterButton)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signInWithEmailPassword(email, password)
            } else {
                showToast("Please fill in all fields")
            }
        }

        registerButton.setOnClickListener {
            (activity as? MainActivity)?.showSignupPage(AdminSignupFragment())
        }

        return view
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    fetchAdminDataAndNavigate(user.uid)
                } else {
                    showToast("Login failed")
                }
            }
            .addOnFailureListener {
                showToast("Login unsuccessful - invalid credentials")
            }
    }


    private fun fetchAdminDataAndNavigate(uid: String) {
        db.collection("admins")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val email = document.getString("email") ?: ""
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""

                    // Store session data with all fields
                    storeLoginSession(email, uid, firstName, lastName)

                    showToast("Login successful!")
                    (activity as? MainActivity)?.showDashboard(AdminDashboardFragment())
                } else {
                    auth.signOut()
                    showToast("Login unsuccessful - not an admin account")
                }
            }
            .addOnFailureListener { exception ->
                showToast("Error fetching admin data: ${exception.message}")
                auth.signOut()
            }
    }


    private fun storeLoginSession(email: String, uid: String, firstName: String, lastName: String) {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        sharedPref?.edit()?.apply {
            putString("loginToken", uid)
            putString("adminEmail", email)
            putString("adminFirstName", firstName)
            putString("adminLastName", lastName)
            putBoolean("isAdmin", true)
            apply()
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        auth.currentUser?.let { user ->
            fetchAdminDataAndNavigate(user.uid)
        }
    }
}