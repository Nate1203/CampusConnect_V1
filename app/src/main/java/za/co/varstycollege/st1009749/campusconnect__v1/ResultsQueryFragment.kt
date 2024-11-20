package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentResultsQueryBinding

class ResultsQueryFragment : Fragment() {
    private var _binding: FragmentResultsQueryBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedFileName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsQueryBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        setupViews()
        setupInitialSpinners()
        setupFetchButton()
        setupUploadButton()
        setInputsEnabled(false)

        return binding.root
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.apply {
            // Spinners
            queryTypeSpinner.isEnabled = enabled
            moduleSpinner.isEnabled = enabled
            assessmentTypeSpinner.isEnabled = enabled
            academicPeriodSpinner.isEnabled = enabled

            // Input fields
            descriptionInput.isEnabled = enabled

            // Buttons
            uploadButton.isEnabled = enabled
            submitButton.isEnabled = enabled
        }
    }

    private fun setupViews() {
        arguments?.getString("queryTitle")?.let { title ->
            binding.titleText.text = title
            binding.titleText.setTextColor(ContextCompat.getColor(requireContext(), R.color.electric_blue))
        }

        // Add click listeners for disabled fields
        binding.apply {
            descriptionInput.setOnClickListener {
                if (!it.isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }

            uploadButton.setOnClickListener {
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
        setInputsEnabled(false)
    }

    private fun setupInitialSpinners() {
        // Query Type Setup
        val queryTypes = arrayOf(
            "Please Select",
            "Missing Results",
            "Results Verification",
            "Results Appeal",
            "Supplementary Exam Results",
            "Final Mark Query"
        )
        val queryAdapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, queryTypes)
        (binding.queryTypeSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(queryAdapter)
            setText(queryTypes[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Assessment Type Setup
        val assessmentTypes = arrayOf(
            "Please Select",
            "Test",
            "Assignment",
            "Project",
            "Exam",
            "Supplementary Exam"
        )
        val assessmentAdapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, assessmentTypes)
        (binding.assessmentTypeSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(assessmentAdapter)
            setText(assessmentTypes[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Academic Period Setup
        val academicPeriods = arrayOf(
            "Please Select",
            "Semester 1 - 2024",
            "Semester 2 - 2024",
            "Year-long Module - 2024"
        )
        val academicAdapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, academicPeriods)
        (binding.academicPeriodSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(academicAdapter)
            setText(academicPeriods[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Initial Module Setup
        val initialModules = listOf("Please Select")
        updateModuleSpinner(initialModules)
    }

    private fun updateModuleSpinner(items: List<String>) {
        val moduleAdapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, items)
        (binding.moduleSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(moduleAdapter)
            setText(items[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupUploadButton() {
        binding.uploadButton.setOnClickListener {
            selectFile()
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
                        selectedFileName = displayName ?: "selected_file"
                        Toast.makeText(context, "File selected: $selectedFileName", Toast.LENGTH_SHORT).show()
                        Log.d("FileUpload", "File selected: $selectedFileName")
                    }
                }
            }
        }
    }

    private fun setupFetchButton() {
        binding.fetchDetailsButton.setOnClickListener {
            val studentId = binding.studentIdInput.text.toString()
            if (studentId.isNotEmpty()) {
                fetchStudentDetails()
            } else {
                Toast.makeText(context, "Please enter a Student ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchStudentDetails() {
        val enteredId = binding.studentIdInput.text.toString()
        Log.d("ResultsQueryFragment", "Fetching details for student ID: $enteredId")

        binding.fetchDetailsButton.isEnabled = false

        db.collection("StudentModules")
            .document(enteredId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        // Set basic info from document fields
                        binding.nameInput.setText(document.getString("name") ?: "")
                        binding.surnameInput.setText(document.getString("surname") ?: "")
                        binding.qualificationInput.setText(document.getString("qualification") ?: "")

                        // Handle modules list
                        val modulesList = mutableListOf("Please Select")

                        // Get modules as array
                        @Suppress("UNCHECKED_CAST")
                        val modulesArray = document.get("modules") as? ArrayList<Map<String, Any>>

                        Log.d("ModuleData", "Raw modules data: $modulesArray")

                        modulesArray?.forEach { moduleMap ->
                            val code = moduleMap["code"] as? String
                            val name = moduleMap["name"] as? String
                            if (!code.isNullOrEmpty() && !name.isNullOrEmpty()) {
                                modulesList.add("$code - $name")
                                Log.d("ModuleData", "Added module: $code - $name")
                            }
                        }

                        // Update module dropdown
                        val moduleAdapter = ArrayAdapter(
                            requireContext(),
                            R.layout.spinner_dropdown_item,
                            modulesList
                        )
                        (binding.moduleSpinner as? AutoCompleteTextView)?.apply {
                            setAdapter(moduleAdapter)
                            setText(modulesList[0], false)
                        }

                        setInputsEnabled(true)
                        Toast.makeText(context, "Student details loaded successfully", Toast.LENGTH_SHORT).show()

                    } catch (e: Exception) {
                        Log.e("ResultsQueryFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ResultsQueryFragment", "Error fetching document: ${e.message}", e)
                Toast.makeText(context, "Error fetching student details", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .addOnCompleteListener {
                binding.fetchDetailsButton.isEnabled = true
            }
    }


    private fun submitForm() {
        val studentId = binding.studentIdInput.text.toString()
        val name = binding.nameInput.text.toString()
        val surname = binding.surnameInput.text.toString()
        val qualification = binding.qualificationInput.text.toString()
        val queryType = (binding.queryTypeSpinner as? AutoCompleteTextView)?.text.toString()
        val academicPeriod = (binding.academicPeriodSpinner as? AutoCompleteTextView)?.text.toString()
        val module = (binding.moduleSpinner as? AutoCompleteTextView)?.text.toString()
        val assessmentType = (binding.assessmentTypeSpinner as? AutoCompleteTextView)?.text.toString()
        val description = binding.descriptionInput.text.toString()

        when {
            studentId.isEmpty() -> {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
            }
            queryType == "Please Select" -> {
                Toast.makeText(context, "Please select a query type", Toast.LENGTH_SHORT).show()
            }
            academicPeriod == "Please Select" -> {
                Toast.makeText(context, "Please select an academic period", Toast.LENGTH_SHORT).show()
            }
            module == "Please Select" -> {
                Toast.makeText(context, "Please select a module", Toast.LENGTH_SHORT).show()
            }
            assessmentType == "Please Select" -> {
                Toast.makeText(context, "Please select an assessment type", Toast.LENGTH_SHORT).show()
            }
            description.isEmpty() -> {
                Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val queryName = binding.titleText.text.toString()
                val queryData: Map<String, Any> = hashMapOf(
                    "studentId" to studentId,
                    "name" to name,
                    "surname" to surname,
                    "qualification" to qualification,
                    "queryType" to queryType,
                    "academicPeriod" to academicPeriod,
                    "module" to module,
                    "assessmentType" to assessmentType,
                    "description" to description,
                    "attachedFile" to selectedFileName, // Changed to just use selectedFileName, which will be empty if no file selected
                    "queryCategory" to "Campus Queries",
                    "queryName" to queryName,
                    "status" to "Pending",
                    "dateSubmitted" to FieldValue.serverTimestamp()
                )

                binding.submitButton.isEnabled = false
                val progressDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Submitting Query")
                    .setMessage("Please wait...")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                db.collection("CampusQuery")
                    .add(queryData)
                    .addOnSuccessListener { documentReference ->
                        progressDialog.dismiss()
                        binding.submitButton.isEnabled = true
                        Log.d("SubmitQuery", "Query submitted with ID: ${documentReference.id}")
                        Toast.makeText(context, "Query submitted successfully!", Toast.LENGTH_LONG).show()
                        clearFields()
                        selectedFileName = ""
                        setInputsEnabled(false)
                        binding.studentIdInput.setText("")
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        binding.submitButton.isEnabled = true
                        Toast.makeText(context, "Error submitting query: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("SubmitQuery", "Error submitting query", e)
                    }
            }
        }
    }


    private fun clearFields() {
        binding.apply {
            nameInput.setText("")
            surnameInput.setText("")
            qualificationInput.setText("")
            descriptionInput.setText("")

            // Reset spinners
            queryTypeSpinner.setText("Please Select")
            moduleSpinner.setText("Please Select")
            assessmentTypeSpinner.setText("Please Select")
            academicPeriodSpinner.setText("Please Select")
        }
        selectedFileName = ""
        setInputsEnabled(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 123

        fun newInstance(queryTitle: String) = ResultsQueryFragment().apply {
            arguments = Bundle().apply {
                putString("queryTitle", queryTitle)
            }
        }
    }
}