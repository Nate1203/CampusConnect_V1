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
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentTimetableQueryBinding

class TimetableQueryFragment : Fragment() {
    private var _binding: FragmentTimetableQueryBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedFileName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimetableQueryBinding.inflate(inflater, container, false)
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
            academicPeriodSpinner.isEnabled = enabled
            dayOfWeekSpinner.isEnabled = enabled
            timeSlotSpinner.isEnabled = enabled

            // Input fields
            descriptionInput.isEnabled = enabled

            // Buttons
            uploadButton.isEnabled = enabled
            submitButton.isEnabled = enabled

            // Enable/disable module checkboxes
            moduleCheckboxContainer.children.forEach { view ->
                if (view is CheckBox) {
                    view.isEnabled = enabled
                }
            }
        }
    }

    private fun setupViews() {
        arguments?.getString("queryTitle")?.let { title ->
            binding.titleText.text = title
            binding.titleText.setTextColor(ContextCompat.getColor(requireContext(), R.color.electric_blue))
        }

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
            "Missing Module on Timetable",
            "Timetable Clash",
            "Wrong Venue",
            "Wrong Lecture Time",
            "General Timetable Query"
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

        // Day of Week Setup
        val daysOfWeek = arrayOf(
            "Please Select",
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday"
        )
        val daysAdapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, daysOfWeek)
        (binding.dayOfWeekSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(daysAdapter)
            setText(daysOfWeek[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Time Slot Setup
        val timeSlots = arrayOf(
            "Please Select",
            "08:00 - 09:00",
            "09:00 - 10:00",
            "10:00 - 11:00",
            "11:00 - 12:00",
            "12:00 - 13:00",
            "13:00 - 14:00",
            "14:00 - 15:00",
            "15:00 - 16:00",
            "16:00 - 17:00"
        )
        val timeAdapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, timeSlots)
        (binding.timeSlotSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(timeAdapter)
            setText(timeSlots[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupModuleCheckboxes(modules: List<String>) {
        binding.moduleCheckboxContainer.removeAllViews() // Clear existing checkboxes

        modules.forEach { moduleText ->
            val checkbox = CheckBox(requireContext()).apply {
                text = moduleText
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                isEnabled = false  // Will be enabled after student fetch
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8)
                }
            }
            binding.moduleCheckboxContainer.addView(checkbox)
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
        Log.d("TimetableQueryFragment", "Fetching details for student ID: $enteredId")

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

                        // Handle modules list for checkboxes
                        val modulesList = mutableListOf<String>()

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

                        // Setup checkboxes with fetched modules
                        setupModuleCheckboxes(modulesList)

                        setInputsEnabled(true)
                        Toast.makeText(context, "Student details loaded successfully", Toast.LENGTH_SHORT).show()

                    } catch (e: Exception) {
                        Log.e("TimetableQueryFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("TimetableQueryFragment", "Error fetching document: ${e.message}", e)
                Toast.makeText(context, "Error fetching student details", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .addOnCompleteListener {
                binding.fetchDetailsButton.isEnabled = true
            }
    }

    private fun getSelectedModules(): List<String> {
        return binding.moduleCheckboxContainer.children
            .filterIsInstance<CheckBox>()
            .filter { it.isChecked }
            .map { it.text.toString() }
            .toList()
    }

    private fun submitForm() {
        val studentId = binding.studentIdInput.text.toString()
        val name = binding.nameInput.text.toString()
        val surname = binding.surnameInput.text.toString()
        val qualification = binding.qualificationInput.text.toString()
        val queryType = (binding.queryTypeSpinner as? AutoCompleteTextView)?.text.toString()
        val academicPeriod = (binding.academicPeriodSpinner as? AutoCompleteTextView)?.text.toString()
        val dayOfWeek = (binding.dayOfWeekSpinner as? AutoCompleteTextView)?.text.toString()
        val timeSlot = (binding.timeSlotSpinner as? AutoCompleteTextView)?.text.toString()
        val description = binding.descriptionInput.text.toString()
        val selectedModules = getSelectedModules()

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
            dayOfWeek == "Please Select" -> {
                Toast.makeText(context, "Please select a day of week", Toast.LENGTH_SHORT).show()
            }
            timeSlot == "Please Select" -> {
                Toast.makeText(context, "Please select a time slot", Toast.LENGTH_SHORT).show()
            }
            selectedModules.isEmpty() -> {
                Toast.makeText(context, "Please select at least one affected module", Toast.LENGTH_SHORT).show()
            }
            description.isEmpty() -> {
                Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val queryData: Map<String, Any> = hashMapOf(
                    "studentId" to studentId,
                    "name" to name,
                    "surname" to surname,
                    "qualification" to qualification,
                    "queryType" to queryType,
                    "academicPeriod" to academicPeriod,
                    "dayOfWeek" to dayOfWeek,
                    "timeSlot" to timeSlot,
                    "affectedModules" to selectedModules,
                    "description" to description,
                    "attachedFile" to selectedFileName,
                    "queryName" to binding.titleText.text.toString(),
                    "queryCategory" to "Campus Queries",
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
            moduleCheckboxContainer.removeAllViews()
            descriptionInput.setText("")

            // Reset spinners to initial state
            (queryTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (academicPeriodSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (dayOfWeekSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (timeSlotSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
        }
        setInputsEnabled(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 123

        fun newInstance(queryTitle: String) = TimetableQueryFragment().apply {
            arguments = Bundle().apply {
                putString("queryTitle", queryTitle)
            }
        }
    }
}