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
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentVisaQueryBinding
import java.text.SimpleDateFormat
import java.util.*

class VisaQueryFragment : Fragment() {
    private var _binding: FragmentVisaQueryBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedPassportFileName: String = ""
    private var selectedVisaFileName: String = ""
    private var selectedSupportingDocsFileName: String = ""
    private var currentUploadType: String = ""

    companion object {
        private const val PASSPORT_UPLOAD_REQUEST_CODE = 123
        private const val VISA_UPLOAD_REQUEST_CODE = 124
        private const val SUPPORTING_DOCS_REQUEST_CODE = 125

        fun newInstance(queryTitle: String) = VisaQueryFragment().apply {
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
        _binding = FragmentVisaQueryBinding.inflate(inflater, container, false)
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
        binding.visaExpiryInput.setOnClickListener {
            if (!it.isEnabled) {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val materialDateBuilder = MaterialDatePicker.Builder.datePicker().apply {
                setTitleText("Select Visa Expiry Date")
                setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            }

            val datePicker = materialDateBuilder.build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.visaExpiryInput.setText(dateFormat.format(calendar.time))
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupSpinners() {
        // Query Type Setup
        val queryTypes = arrayOf(
            "Please Select",
            "Study Visa Letter Request",
            "Student Visa Extension Letter",
            "Letter of Good Standing",
            "Letter of Conduct",
            "Visa Status Confirmation Letter",
            "Bank Letter for Visa",
            "General Visa Enquiry"
        )
        setupSpinner(binding.queryTypeSpinner, queryTypes)

        // Document Type Setup
        val documentTypes = arrayOf(
            "Please Select",
            "New Study Visa Application",
            "Visa Extension",
            "Change of Status",
            "Visa Transfer",
            "Work Visa Endorsement",
            "Critical Skills Visa"
        )
        setupSpinner(binding.documentTypeSpinner, documentTypes)

        // Visa Status Setup
        val visaStatus = arrayOf(
            "Please Select",
            "Current Visa Valid",
            "Visa Expiring Soon",
            "Visa Expired",
            "Awaiting New Visa",
            "First Time Application"
        )
        setupSpinner(binding.visaStatusSpinner, visaStatus)

        // Purpose Setup
        val purposes = arrayOf(
            "Please Select",
            "Embassy/Consulate Submission",
            "Home Affairs Submission",
            "Bank Requirement",
            "Immigration Verification",
            "Employment Purpose",
            "Other"
        )
        setupSpinner(binding.purposeSpinner, purposes)

        // Urgency Level Setup
        val urgencyLevels = arrayOf(
            "Please Select",
            "Urgent - Required within 24 hours",
            "High Priority - Required within 3 days",
            "Standard - Required within 5 days",
            "Non-urgent - Required within 7 days"
        )
        setupSpinner(binding.urgencyLevelSpinner, urgencyLevels)
    }

    private fun setupSpinner(spinner: AutoCompleteTextView, items: Array<String>) {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        spinner.apply {
            setAdapter(adapter)
            setText(items[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(
                        context,
                        "Please fetch student details first",
                        Toast.LENGTH_SHORT
                    ).show()
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

            passportUploadButton.setOnClickListener {
                if (!it.isEnabled) {
                    Toast.makeText(
                        context,
                        "Please fetch student details first",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    currentUploadType = "passport"
                    selectFile(PASSPORT_UPLOAD_REQUEST_CODE)
                }
            }

            visaUploadButton.setOnClickListener {
                if (!it.isEnabled) {
                    Toast.makeText(
                        context,
                        "Please fetch student details first",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    currentUploadType = "visa"
                    selectFile(VISA_UPLOAD_REQUEST_CODE)
                }
            }

            supportingDocsButton.setOnClickListener {
                if (!it.isEnabled) {
                    Toast.makeText(
                        context,
                        "Please fetch student details first",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    currentUploadType = "supporting"
                    selectFile(SUPPORTING_DOCS_REQUEST_CODE)
                }
            }

            submitButton.setOnClickListener {
                if (!it.isEnabled) {
                    Toast.makeText(
                        context,
                        "Please fetch student details first",
                        Toast.LENGTH_SHORT
                    ).show()
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
            documentTypeSpinner.isEnabled = enabled
            visaStatusSpinner.isEnabled = enabled
            purposeSpinner.isEnabled = enabled
            urgencyLevelSpinner.isEnabled = enabled

            // Input fields
            passportNumberInput.isEnabled = enabled
            visaExpiryInput.isEnabled = enabled
            additionalInfoInput.isEnabled = enabled
            descriptionInput.isEnabled = enabled

            // Upload buttons
            passportUploadButton.isEnabled = enabled
            visaUploadButton.isEnabled = enabled
            supportingDocsButton.isEnabled = enabled
            submitButton.isEnabled = enabled
        }
    }

    private fun selectFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "image/*"
                )
            )
        }
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex =
                            it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                        val displayName = it.getString(displayNameIndex)

                        when (requestCode) {
                            PASSPORT_UPLOAD_REQUEST_CODE -> {
                                selectedPassportFileName = displayName ?: "passport_file"
                                binding.passportUploadButton.text = "Passport Copy Selected"
                            }

                            VISA_UPLOAD_REQUEST_CODE -> {
                                selectedVisaFileName = displayName ?: "visa_file"
                                binding.visaUploadButton.text = "Visa Copy Selected"
                            }

                            SUPPORTING_DOCS_REQUEST_CODE -> {
                                selectedSupportingDocsFileName = displayName ?: "supporting_docs"
                                binding.supportingDocsButton.text = "Supporting Docs Selected"
                            }
                        }
                        Toast.makeText(context, "File selected successfully", Toast.LENGTH_SHORT)
                            .show()
                        Log.d("FileUpload", "File selected: $displayName")
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
                        Toast.makeText(
                            context,
                            "Student details loaded successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (e: Exception) {
                        Log.e("VisaQueryFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT)
                            .show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("VisaQueryFragment", "Error fetching document: ${e.message}", e)
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
            val documentType = (documentTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val visaStatus = (visaStatusSpinner as? AutoCompleteTextView)?.text.toString()
            val purpose = (purposeSpinner as? AutoCompleteTextView)?.text.toString()
            val urgencyLevel = (urgencyLevelSpinner as? AutoCompleteTextView)?.text.toString()
            val passportNumber = passportNumberInput.text.toString()
            val visaExpiry = visaExpiryInput.text.toString()
            val additionalInfo = additionalInfoInput.text.toString()
            val description = descriptionInput.text.toString()

            when {
                studentId.isEmpty() -> {
                    Toast.makeText(
                        context,
                        "Please fetch student details first",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                queryType == "Please Select" -> {
                    Toast.makeText(context, "Please select a query type", Toast.LENGTH_SHORT).show()
                }

                documentType == "Please Select" -> {
                    Toast.makeText(context, "Please select a document type", Toast.LENGTH_SHORT)
                        .show()
                }

                visaStatus == "Please Select" -> {
                    Toast.makeText(
                        context,
                        "Please select your current visa status",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                purpose == "Please Select" -> {
                    Toast.makeText(
                        context,
                        "Please select the purpose of the letter",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                urgencyLevel == "Please Select" -> {
                    Toast.makeText(context, "Please select an urgency level", Toast.LENGTH_SHORT)
                        .show()
                }

                passportNumber.isEmpty() -> {
                    Toast.makeText(context, "Please enter your passport number", Toast.LENGTH_SHORT)
                        .show()
                }

                selectedPassportFileName.isEmpty() -> {
                    Toast.makeText(
                        context,
                        "Please upload a copy of your passport",
                        Toast.LENGTH_SHORT
                    ).show()
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
                        "documentType" to documentType,
                        "visaStatus" to visaStatus,
                        "purpose" to purpose,
                        "urgencyLevel" to urgencyLevel,
                        "passportNumber" to passportNumber,
                        "visaExpiryDate" to visaExpiry,
                        "additionalInfo" to additionalInfo,
                        "description" to description,
                        "passportCopy" to selectedPassportFileName,
                        "visaCopy" to selectedVisaFileName,
                        "supportingDocs" to selectedSupportingDocsFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "Visa Query"),
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
                            Toast.makeText(
                                context,
                                "Query submitted successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                            clearFields()
                            resetFileSelections()
                            setInputsEnabled(false)
                            studentIdInput.setText("")
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Toast.makeText(
                                context,
                                "Error submitting query: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("SubmitQuery", "Error submitting query", e)
                        }
                }
            }
        }
    }

    private fun resetFileSelections() {
        binding.apply {
            passportUploadButton.text = "Upload Passport Copy"
            visaUploadButton.text = "Upload Current Visa Copy"
            supportingDocsButton.text = "Upload Supporting Documents"
        }
        selectedPassportFileName = ""
        selectedVisaFileName = ""
        selectedSupportingDocsFileName = ""
    }

    private fun clearFields() {
        binding.apply {
            nameInput.setText("")
            surnameInput.setText("")
            qualificationInput.setText("")
            passportNumberInput.setText("")
            visaExpiryInput.setText("")
            additionalInfoInput.setText("")
            descriptionInput.setText("")

            // Reset spinners
            (queryTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (documentTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (visaStatusSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (purposeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (urgencyLevelSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
        }
        resetFileSelections()
        setInputsEnabled(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
