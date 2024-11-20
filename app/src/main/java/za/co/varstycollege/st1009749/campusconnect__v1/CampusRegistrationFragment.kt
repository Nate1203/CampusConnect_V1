package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class CampusRegistrationFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var studentId: String

    private lateinit var studentIdInput: TextInputEditText
    private lateinit var nameInput: TextInputEditText
    private lateinit var surnameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var qualificationInput: AutoCompleteTextView
    private lateinit var campusInput: AutoCompleteTextView
    private lateinit var studyTypeInput: AutoCompleteTextView
    private lateinit var deliveryModeInput: AutoCompleteTextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_campus_registration, container, false)

        db = FirebaseFirestore.getInstance()
        studentId = arguments?.getString("studentId") ?: ""

        initializeViews(view)
        setupDropdowns()

        view.findViewById<View>(R.id.fetchDetailsButton).setOnClickListener { fetchStudentDetails() }
        view.findViewById<View>(R.id.proceedToModulesButton).setOnClickListener { saveDetailsAndProceed() }

        showStudentIdDialog()

        return view
    }

    private fun initializeViews(view: View) {
        studentIdInput = view.findViewById(R.id.studentIdInput)
        nameInput = view.findViewById(R.id.nameInput)
        surnameInput = view.findViewById(R.id.surnameInput)
        emailInput = view.findViewById(R.id.emailInput)
        qualificationInput = view.findViewById(R.id.qualificationInput)
        campusInput = view.findViewById(R.id.campusInput)
        studyTypeInput = view.findViewById(R.id.studyTypeInput)
        deliveryModeInput = view.findViewById(R.id.deliveryModeInput)

        studentIdInput.setText(studentId)
    }

    private fun setupDropdowns() {
        val qualifications = arrayOf(
            "BCAD - Bachelor of Computer and Information Sciences in Application Development",
            "BSIT - Bachelor of Science in Information Technology",
            "BDSS - Bachelor of Data Science and Analytics",
            "BCOM - Bachelor of Commerce in Digital Marketing",
            "BADA - Bachelor of Arts in Digital Arts"
        )
        val campuses = arrayOf("Sandton", "Pretoria", "Durban", "Cape Town")
        val studyTypes = arrayOf("Full Time", "Part Time")
        val deliveryModes = arrayOf("Contact", "Distance", "Online")

        setupDropdown(qualificationInput, qualifications)
        setupDropdown(campusInput, campuses)
        setupDropdown(studyTypeInput, studyTypes)
        setupDropdown(deliveryModeInput, deliveryModes)
    }

    private fun setupDropdown(autoCompleteTextView: AutoCompleteTextView, items: Array<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        autoCompleteTextView.setAdapter(adapter)
    }

    private fun showStudentIdDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Your Student ID")
            .setMessage("Your Student ID is: $studentId\n\nPlease write this down and keep it safe.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun fetchStudentDetails() {
        val enteredId = studentIdInput.text.toString()
        db.collection("students")
            .whereEqualTo("studentId", enteredId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "No student found with this ID", Toast.LENGTH_SHORT).show()
                } else {
                    val student = documents.documents[0]
                    nameInput.setText(student.getString("firstName"))
                    surnameInput.setText(student.getString("lastName"))
                    emailInput.setText(student.getString("email"))
                    Toast.makeText(context, "Student details fetched successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching student details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveDetailsAndProceed() {
        val campusRegistrationData = hashMapOf(
            "studentId" to studentIdInput.text.toString(),
            "name" to nameInput.text.toString(),
            "surname" to surnameInput.text.toString(),
            "email" to emailInput.text.toString(),
            "qualification" to qualificationInput.text.toString(),
            "campus" to campusInput.text.toString(),
            "studyType" to studyTypeInput.text.toString(),
            "deliveryMode" to deliveryModeInput.text.toString(),
            "timestamp" to Timestamp.now()
        )

        db.collection("CampusRegistration")
            .document(studentId)
            .set(campusRegistrationData)
            .addOnSuccessListener {
                Toast.makeText(context, "Registration details saved successfully", Toast.LENGTH_SHORT).show()
                // Navigate to module selection
                val moduleSelectionFragment = ModuleSelectionFragment.newInstance(
                    studentId,
                    nameInput.text.toString(),
                    surnameInput.text.toString(),
                    qualificationInput.text.toString()
                )
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, moduleSelectionFragment)
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving registration details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        fun newInstance(studentId: String) = CampusRegistrationFragment().apply {
            arguments = Bundle().apply {
                putString("studentId", studentId)
            }
        }
    }
}