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
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentPasQueryBinding
import java.text.SimpleDateFormat
import java.util.*

class PasQueryFragment : Fragment() {
    private var _binding: FragmentPasQueryBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedFileName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasQueryBinding.inflate(inflater, container, false)
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
                setTextColor(ContextCompat.getColor(requireContext(), R.color.electric_blue))
            }
        }
    }

    private fun setupDatePicker() {
        binding.assessmentDateInput.setOnClickListener {
            if (!it.isEnabled) {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val materialDateBuilder = MaterialDatePicker.Builder.datePicker().apply {
                setTitleText("Select Assessment Date")
                setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            }

            val datePicker = materialDateBuilder.build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.assessmentDateInput.setText(dateFormat.format(calendar.time))
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupSpinners() {
        // Query Type Setup
        val queryTypes = arrayOf(
            "Please Select",
            "Missing Assessment Schedule",
            "Incorrect Assessment Date",
            "Assessment Deadline Query",
            "Missing Module on PAS",
            "PAS Access Issue",
            "General PAS Query",
            "Schedule Clash",
            "Assessment Type Query"
        )
        setupSpinner(binding.queryTypeSpinner, queryTypes)

        // Academic Period Setup
        val academicPeriods = arrayOf(
            "Please Select",
            "Semester 1 - 2024",
            "Semester 2 - 2024",
            "Year-long Module - 2024"
        )
        setupSpinner(binding.academicPeriodSpinner, academicPeriods)

        // Assessment Type Setup
        val assessmentTypes = arrayOf(
            "Please Select",
            "Assignment",
            "Test",
            "Project",
            "Portfolio",
            "Practical Assessment",
            "Written Exam",
            "Online Quiz",
            "Presentation"
        )
        setupSpinner(binding.assessmentTypeSpinner, assessmentTypes)

        // Priority Level Setup
        val priorityLevels = arrayOf(
            "Please Select",
            "High - Upcoming Assessment within 48 hours",
            "Medium - Assessment within 1 week",
            "Low - General Query"
        )
        setupSpinner(binding.priorityLevelSpinner, priorityLevels)

        // Initial Module Setup
        val initialModules = listOf("Please Select")
        updateModuleSpinner(initialModules)
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
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.apply {
            // Spinners
            queryTypeSpinner.isEnabled = enabled
            moduleSpinner.isEnabled = enabled
            assessmentTypeSpinner.isEnabled = enabled
            academicPeriodSpinner.isEnabled = enabled
            priorityLevelSpinner.isEnabled = enabled

            // Input fields
            assessmentDateInput.isEnabled = enabled
            descriptionInput.isEnabled = enabled

            // Buttons
            uploadButton.isEnabled = enabled
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
                        selectedFileName = displayName ?: "selected_file"
                        Toast.makeText(context, "File selected: $selectedFileName", Toast.LENGTH_SHORT).show()
                        Log.d("FileUpload", "File selected: $selectedFileName")
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
                        Log.e("PasQueryFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PasQueryFragment", "Error fetching document: ${e.message}", e)
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
            val queryType = (queryTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val academicPeriod = (academicPeriodSpinner as? AutoCompleteTextView)?.text.toString()
            val module = (moduleSpinner as? AutoCompleteTextView)?.text.toString()
            val assessmentType = (assessmentTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val priorityLevel = (priorityLevelSpinner as? AutoCompleteTextView)?.text.toString()
            val assessmentDate = assessmentDateInput.text.toString()
            val description = descriptionInput.text.toString()

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
                priorityLevel == "Please Select" -> {
                    Toast.makeText(context, "Please select a priority level", Toast.LENGTH_SHORT).show()
                }
                description.isEmpty() -> {
                    Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    submitButton.isEnabled = false
                    val progressDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Submitting Query")
                        .setMessage("Please wait...")
                        .setCancelable(false)
                        .create()
                    progressDialog.show()

                    val queryData = hashMapOf(
                        "studentId" to studentId,
                        "name" to nameInput.text.toString(),
                        "surname" to surnameInput.text.toString(),
                        "qualification" to qualificationInput.text.toString(),
                        "queryType" to queryType,
                        "academicPeriod" to academicPeriod,
                        "module" to module,
                        "assessmentType" to assessmentType,
                        "priorityLevel" to priorityLevel,
                        "assessmentDate" to assessmentDate,
                        "description" to description,
                        "attachedFile" to selectedFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "PAS Query"),
                        "queryCategory" to "Campus Queries",
                        "status" to "Pending",
                        "dateSubmitted" to FieldValue.serverTimestamp()
                    )

                    db.collection("CampusQuery")
                        .add(queryData)
                        .addOnSuccessListener { documentReference ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Log.d("SubmitQuery", "Query submitted with ID: ${documentReference.id}")
                            Toast.makeText(context, "Query submitted successfully!", Toast.LENGTH_LONG).show()
                            clearFields()
                            selectedFileName = ""
                            setInputsEnabled(false)
                            studentIdInput.setText("")
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Toast.makeText(context, "Error submitting query: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("SubmitQuery", "Error submitting query", e)
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
            assessmentDateInput.setText("")
            descriptionInput.setText("")

            // Reset spinners
            (queryTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (academicPeriodSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (moduleSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (assessmentTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (priorityLevelSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
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

        fun newInstance(queryTitle: String) = PasQueryFragment().apply {
            arguments = Bundle().apply {
                putString("queryTitle", queryTitle)
            }
        }
    }
}