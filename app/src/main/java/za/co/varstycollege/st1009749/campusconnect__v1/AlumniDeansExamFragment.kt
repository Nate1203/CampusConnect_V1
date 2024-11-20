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
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentAlumniDeansExamBinding
import java.text.SimpleDateFormat
import java.util.*

class AlumniDeansExamFragment : Fragment() {
    private var _binding: FragmentAlumniDeansExamBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedDocsFileName: String = ""

    companion object {
        const val FILE_PICKER_REQUEST_CODE = 123

        fun newInstance(queryTitle: String) = AlumniDeansExamFragment().apply {
            arguments = Bundle().apply {
                putString("queryTitle", queryTitle)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlumniDeansExamBinding.inflate(inflater, container, false)
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
                setTextColor(ContextCompat.getColor(requireContext(), R.color.darker_purple))
            }
        }
    }

    private fun setupDatePicker() {
        binding.lastEnrollmentDateInput.setOnClickListener {
            if (!it.isEnabled) {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val materialDateBuilder = MaterialDatePicker.Builder.datePicker().apply {
                setTitleText("Select Last Enrollment Date")
                setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            }

            val datePicker = materialDateBuilder.build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.lastEnrollmentDateInput.setText(dateFormat.format(calendar.time))
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

        // Module Setup
        val initialModules = listOf("Please Select")
        updateModuleSpinner(initialModules)

        // Exam Type Setup
        val examTypes = arrayOf(
            "Please Select",
            "Final Opportunity",
            "Special Exam",
            "Supplementary Exam",
            "Alumni Re-write",
            "Completion Exam"
        )
        setupSpinner(binding.examTypeSpinner, examTypes)

        // Reason Setup
        val reasons = arrayOf(
            "Please Select",
            "Previous Failed Attempts",
            "Module Required for Completion",
            "Professional Requirement",
            "Career Advancement",
            "Course Progression",
            "Other"
        )
        setupSpinner(binding.reasonSpinner, reasons)
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

    private fun updateModuleSpinner(items: List<String>) {
        val moduleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        binding.moduleSpinner.apply {
            setAdapter(moduleAdapter)
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
            moduleSpinner.isEnabled = enabled
            examTypeSpinner.isEnabled = enabled
            reasonSpinner.isEnabled = enabled

            // Input fields
            lastEnrollmentDateInput.isEnabled = enabled
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

                        // Handle modules list
                        val modulesList = mutableListOf("Please Select")

                        @Suppress("UNCHECKED_CAST")
                        val modulesArray = document.get("modules") as? ArrayList<Map<String, Any>>

                        modulesArray?.forEach { moduleMap ->
                            val code = moduleMap["code"] as? String
                            val name = moduleMap["name"] as? String
                            if (!code.isNullOrEmpty() && !name.isNullOrEmpty()) {
                                modulesList.add("$code - $name")
                            }
                        }

                        updateModuleSpinner(modulesList)
                        setInputsEnabled(true)
                        Toast.makeText(context, "Student details loaded successfully", Toast.LENGTH_SHORT).show()

                    } catch (e: Exception) {
                        Log.e("AlumniDeansExamFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("AlumniDeansExamFragment", "Error fetching document: ${e.message}", e)
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
            val module = (moduleSpinner as? AutoCompleteTextView)?.text.toString()
            val examType = (examTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val reason = (reasonSpinner as? AutoCompleteTextView)?.text.toString()
            val lastEnrollmentDate = lastEnrollmentDateInput.text.toString()
            val explanation = explanationInput.text.toString()

            when {
                studentId.isEmpty() -> {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
                academicPeriod == "Please Select" -> {
                    Toast.makeText(context, "Please select an academic period", Toast.LENGTH_SHORT).show()
                }
                module == "Please Select" -> {
                    Toast.makeText(context, "Please select a module", Toast.LENGTH_SHORT).show()
                }
                examType == "Please Select" -> {
                    Toast.makeText(context, "Please select an exam type", Toast.LENGTH_SHORT).show()
                }
                reason == "Please Select" -> {
                    Toast.makeText(context, "Please select a reason", Toast.LENGTH_SHORT).show()
                }
                lastEnrollmentDate.isEmpty() -> {
                    Toast.makeText(context, "Please select your last enrollment date", Toast.LENGTH_SHORT).show()
                }
                explanation.isEmpty() -> {
                    Toast.makeText(context, "Please provide a detailed explanation", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    submitButton.isEnabled = false
                    val progressDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Submitting Request")
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
                        "module" to module,
                        "examType" to examType,
                        "reason" to reason,
                        "lastEnrollmentDate" to lastEnrollmentDate,
                        "description" to explanation,
                        "supportingDocs" to selectedDocsFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "Alumni Dean's Exam Request"),
                        "queryCategory" to "Alumni Student Queries",
                        "queryType" to "Alumni Deans Exam",
                        "status" to "Pending",
                        "dateSubmitted" to FieldValue.serverTimestamp()
                    )

                    db.collection("AlumniQuery")
                        .add(queryData)
                        .addOnSuccessListener { documentReference ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Log.d("SubmitQuery", "Query submitted with ID: ${documentReference.id}")
                            Toast.makeText(context, "Request submitted successfully!", Toast.LENGTH_LONG).show()
                            clearFields()
                            selectedDocsFileName = ""
                            setInputsEnabled(false)
                            studentIdInput.setText("")
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Toast.makeText(context, "Error submitting request: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("SubmitQuery", "Error submitting request", e)
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
            lastEnrollmentDateInput.setText("")
            explanationInput.setText("")

            // Reset spinners
            (academicPeriodSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (moduleSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (examTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (reasonSpinner as? AutoCompleteTextView)?.setText("Please Select", false)

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
}