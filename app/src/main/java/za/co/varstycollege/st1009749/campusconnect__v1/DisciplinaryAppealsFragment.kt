package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentDisciplinaryAppealsBinding
import java.text.SimpleDateFormat
import java.util.*

class DisciplinaryAppealsFragment : Fragment() {
    private var _binding: FragmentDisciplinaryAppealsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedDocsFileName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisciplinaryAppealsBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupSpinners()
        setupButtons()
        setupDatePicker()
        setInputsEnabled(false)

        return binding.root
    }

    private fun initializeViews() {
        arguments?.getString("queryTitle")?.let { title ->
            binding.titleText.apply {
                text = title
                setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_pink))
            }
        }
    }

    private fun setupDatePicker() {
        binding.incidentDateInput.setOnClickListener {
            if (!it.isEnabled) {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val materialDateBuilder = MaterialDatePicker.Builder.datePicker().apply {
                setTitleText("Select Incident Date")
                setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            }

            val datePicker = materialDateBuilder.build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.incidentDateInput.setText(dateFormat.format(calendar.time))
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupSpinners() {
        // Academic Period Setup
        val academicPeriods = arrayOf(
            "Please Select",
            "Semester 1 - 2024",
            "Semester 2 - 2024",
            "Year-long Module - 2024"
        )
        setupSpinner(binding.academicPeriodSpinner, academicPeriods)

        // Incident Type Setup
        val incidentTypes = arrayOf(
            "Please Select",
            "Academic Misconduct",
            "Behavioral Misconduct",
            "Plagiarism",
            "Attendance Violation",
            "Code of Conduct Violation",
            "Exam Rules Violation",
            "Other Disciplinary Issue"
        )
        setupSpinner(binding.incidentTypeSpinner, incidentTypes)

        // Appeal Type Setup
        val appealTypes = arrayOf(
            "Please Select",
            "Severity of Penalty",
            "New Evidence",
            "Procedural Error",
            "Mitigating Circumstances",
            "Request for Reconsideration",
            "Other Appeal Grounds"
        )
        setupSpinner(binding.appealTypeSpinner, appealTypes)
    }

    private fun setupSpinner(spinner: AutoCompleteTextView, items: Array<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        spinner.apply {
            setAdapter(adapter)
            setText(items[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButtons() {
        binding.apply {
            fetchDetailsButton.setOnClickListener {
                val studentId = studentIdInput.text.toString()
                if (studentId.isNotEmpty()) {
                    fetchStudentDetails()
                } else {
                    Toast.makeText(context, "Please enter a Student ID", Toast.LENGTH_SHORT).show()
                }
            }

            supportingDocsButton.setOnClickListener {
                if (!it.isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                } else {
                    selectFile()
                }
            }

            submitButton.setOnClickListener {
                if (!it.isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                } else {
                    submitForm()
                }
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.apply {
            // Spinners
            academicPeriodSpinner.isEnabled = enabled
            incidentTypeSpinner.isEnabled = enabled
            appealTypeSpinner.isEnabled = enabled

            // Input fields
            originalDecisionInput.isEnabled = enabled
            incidentDateInput.isEnabled = enabled
            explanationInput.isEnabled = enabled

            // Buttons
            supportingDocsButton.isEnabled = enabled
            submitButton.isEnabled = enabled
        }
    }

    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/*"
            ))
        }
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                        val displayName = it.getString(displayNameIndex)
                        selectedDocsFileName = displayName ?: "supporting_docs"
                        binding.supportingDocsButton.text = "Documentation Selected"
                        Toast.makeText(context, "File selected: $selectedDocsFileName", Toast.LENGTH_SHORT).show()
                        Log.d("FileUpload", "File selected: $selectedDocsFileName")
                    }
                }
            }
        }
    }

    private fun fetchStudentDetails() {
        val enteredId = binding.studentIdInput.text.toString()
        if (enteredId.isEmpty()) {
            Toast.makeText(context, "Please enter a Student ID", Toast.LENGTH_SHORT).show()
            return
        }

        binding.fetchDetailsButton.isEnabled = false

        db.collection("StudentModules")
            .document(enteredId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        binding.apply {
                            nameInput.setText(document.getString("name") ?: "")
                            surnameInput.setText(document.getString("surname") ?: "")
                            qualificationInput.setText(document.getString("qualification") ?: "")
                        }

                        setInputsEnabled(true)
                        Toast.makeText(context, "Student details loaded successfully", Toast.LENGTH_SHORT).show()

                    } catch (e: Exception) {
                        Log.e("DisciplinaryAppealsFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DisciplinaryAppealsFragment", "Error fetching document: ${e.message}", e)
                Toast.makeText(context, "Error fetching student details", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .addOnCompleteListener {
                binding.fetchDetailsButton.isEnabled = true
            }
    }

    private fun submitForm() {
        binding.apply {
            val studentId = studentIdInput.text.toString()
            val academicPeriod = (academicPeriodSpinner as? AutoCompleteTextView)?.text.toString()
            val incidentType = (incidentTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val appealType = (appealTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val originalDecision = originalDecisionInput.text.toString()
            val incidentDate = incidentDateInput.text.toString()
            val explanation = explanationInput.text.toString()

            when {
                studentId.isEmpty() -> {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
                academicPeriod == "Please Select" -> {
                    Toast.makeText(context, "Please select an academic period", Toast.LENGTH_SHORT).show()
                }
                incidentType == "Please Select" -> {
                    Toast.makeText(context, "Please select an incident type", Toast.LENGTH_SHORT).show()
                }
                appealType == "Please Select" -> {
                    Toast.makeText(context, "Please select an appeal type", Toast.LENGTH_SHORT).show()
                }
                originalDecision.isEmpty() -> {
                    Toast.makeText(context, "Please enter the original disciplinary decision", Toast.LENGTH_SHORT).show()
                }
                incidentDate.isEmpty() -> {
                    Toast.makeText(context, "Please select the incident date", Toast.LENGTH_SHORT).show()
                }
                explanation.isEmpty() -> {
                    Toast.makeText(context, "Please provide grounds for appeal", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    submitButton.isEnabled = false
                    val progressDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Submitting Appeal")
                        .setMessage("Please wait...")
                        .setCancelable(false)
                        .create()
                    progressDialog.show()

                    val queryData = hashMapOf(
                        "studentId" to studentId,
                        "name" to nameInput.text.toString(),
                        "surname" to surnameInput.text.toString(),
                        "qualification" to qualificationInput.text.toString(),
                        "academicPeriod" to academicPeriod,
                        "incidentType" to incidentType,
                        "appealType" to appealType,
                        "originalDecision" to originalDecision,
                        "incidentDate" to incidentDate,
                        "description" to explanation,
                        "supportingDocs" to selectedDocsFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "Disciplinary Appeal Request"),
                        "queryCategory" to "Student Hub Queries",
                        "queryType" to "Disciplinary Appeal",
                        "status" to "Pending",
                        "dateSubmitted" to FieldValue.serverTimestamp()
                    )

                    db.collection("StudentHubQuery")
                        .add(queryData)
                        .addOnSuccessListener { documentReference ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Log.d("SubmitQuery", "Query submitted with ID: ${documentReference.id}")
                            Toast.makeText(context, "Appeal submitted successfully!", Toast.LENGTH_LONG).show()
                            clearFields()
                            selectedDocsFileName = ""
                            setInputsEnabled(false)
                            studentIdInput.setText("")
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Toast.makeText(context, "Error submitting appeal: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("SubmitQuery", "Error submitting appeal", e)
                        }
                }
            }
        }
    }

    private fun clearFields() {
        binding.apply {
            nameInput.setText("")
            surnameInput.setText("")
            qualificationInput.setText("")
            originalDecisionInput.setText("")
            incidentDateInput.setText("")
            explanationInput.setText("")

            // Reset spinners
            (academicPeriodSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (incidentTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (appealTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)

            // Reset file upload button text
            supportingDocsButton.text = "Upload Supporting Documents"
        }
        selectedDocsFileName = ""
        setInputsEnabled(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 123

        fun newInstance(queryTitle: String) = DisciplinaryAppealsFragment().apply {
            arguments = Bundle().apply {
                putString("queryTitle", queryTitle)
            }
        }
    }
}