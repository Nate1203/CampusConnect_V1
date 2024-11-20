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
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentExternalCreditBinding
import java.text.SimpleDateFormat
import java.util.*

class ExternalCreditFragment : Fragment() {
    private var _binding: FragmentExternalCreditBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedDocsFileName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExternalCreditBinding.inflate(inflater, container, false)
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
        binding.completionDateInput.setOnClickListener {
            if (!it.isEnabled) {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val materialDateBuilder = MaterialDatePicker.Builder.datePicker().apply {
                setTitleText("Select Module Completion Date")
                setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            }

            val datePicker = materialDateBuilder.build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.completionDateInput.setText(dateFormat.format(calendar.time))
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

        // Request Type Setup
        val requestTypes = arrayOf(
            "Please Select",
            "Credit Transfer",
            "Module Exemption",
            "Recognition of Prior Learning",
            "International Qualification Credit",
            "Professional Certification Credit"
        )
        setupSpinner(binding.requestTypeSpinner, requestTypes)

        // Initial Target Module Setup
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
        binding.targetModuleSpinner.apply {
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
            requestTypeSpinner.isEnabled = enabled
            targetModuleSpinner.isEnabled = enabled

            // Input fields
            previousInstitutionInput.isEnabled = enabled
            previousQualificationInput.isEnabled = enabled
            previousModuleInput.isEnabled = enabled
            completionDateInput.isEnabled = enabled
            finalMarkInput.isEnabled = enabled
            additionalInfoInput.isEnabled = enabled

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
                        selectedDocsFileName = displayName ?: "academic_records"
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
                        Log.e("ExternalCreditFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ExternalCreditFragment", "Error fetching document: ${e.message}", e)
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
            val requestType = (requestTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val targetModule = (targetModuleSpinner as? AutoCompleteTextView)?.text.toString()
            val previousInstitution = previousInstitutionInput.text.toString()
            val previousQualification = previousQualificationInput.text.toString()
            val previousModule = previousModuleInput.text.toString()
            val completionDate = completionDateInput.text.toString()
            val finalMark = finalMarkInput.text.toString()
            val additionalInfo = additionalInfoInput.text.toString()

            when {
                studentId.isEmpty() -> {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
                academicPeriod == "Please Select" -> {
                    Toast.makeText(context, "Please select an academic period", Toast.LENGTH_SHORT).show()
                }
                requestType == "Please Select" -> {
                    Toast.makeText(context, "Please select a request type", Toast.LENGTH_SHORT).show()
                }
                targetModule == "Please Select" -> {
                    Toast.makeText(context, "Please select a target module", Toast.LENGTH_SHORT).show()
                }
                previousInstitution.isEmpty() -> {
                    Toast.makeText(context, "Please enter the previous institution name", Toast.LENGTH_SHORT).show()
                }
                previousQualification.isEmpty() -> {
                    Toast.makeText(context, "Please enter the previous qualification", Toast.LENGTH_SHORT).show()
                }
                previousModule.isEmpty() -> {
                    Toast.makeText(context, "Please enter the previous module name", Toast.LENGTH_SHORT).show()
                }
                completionDate.isEmpty() -> {
                    Toast.makeText(context, "Please select the completion date", Toast.LENGTH_SHORT).show()
                }
                finalMark.isEmpty() -> {
                    Toast.makeText(context, "Please enter the final mark", Toast.LENGTH_SHORT).show()
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
                        "requestType" to requestType,
                        "targetModule" to targetModule,
                        "previousInstitution" to previousInstitution,
                        "previousQualification" to previousQualification,
                        "previousModule" to previousModule,
                        "completionDate" to completionDate,
                        "finalMark" to finalMark,
                        "additionalInfo" to additionalInfo,
                        "supportingDocs" to selectedDocsFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "External Credit Request"),
                        "queryCategory" to "Student Hub Queries",
                        "queryType" to "External Credit",
                        "status" to "Pending",
                        "dateSubmitted" to FieldValue.serverTimestamp()
                    )

                    db.collection("StudentHubQuery")
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
            previousInstitutionInput.setText("")
            previousQualificationInput.setText("")
            previousModuleInput.setText("")
            completionDateInput.setText("")
            finalMarkInput.setText("")
            additionalInfoInput.setText("")

            // Reset spinners
            (academicPeriodSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (requestTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (targetModuleSpinner as? AutoCompleteTextView)?.setText("Please Select", false)

            // Reset file upload button text
            supportingDocsButton.text = "Upload Academic Records"
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

        fun newInstance(queryTitle: String) = ExternalCreditFragment().apply {
            arguments = Bundle().apply {
                putString("queryTitle", queryTitle)
            }
        }
    }
}