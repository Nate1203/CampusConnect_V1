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
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentGraduationConfirmationBinding
import java.text.SimpleDateFormat
import java.util.*

class GraduationConfirmationFragment : Fragment() {
    private var _binding: FragmentGraduationConfirmationBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedDocsFileName: String = ""

    companion object {
        const val FILE_PICKER_REQUEST_CODE = 123

        fun newInstance(queryTitle: String) = GraduationConfirmationFragment().apply {
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
        _binding = FragmentGraduationConfirmationBinding.inflate(inflater, container, false)
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
        binding.graduationDateInput.setOnClickListener {
            if (!it.isEnabled) {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val materialDateBuilder = MaterialDatePicker.Builder.datePicker().apply {
                setTitleText("Select Graduation Date")
                setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            }

            val datePicker = materialDateBuilder.build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.graduationDateInput.setText(dateFormat.format(calendar.time))
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupSpinners() {
        // Document Type Setup
        val documentTypes = arrayOf(
            "Please Select",
            "Graduation Certificate",
            "Academic Transcript",
            "Qualification Verification",
            "Academic Record"
        )
        setupSpinner(binding.documentTypeSpinner, documentTypes)

        // Graduation Year Setup
        val graduationYears = arrayOf(
            "Please Select",
            "2023",
            "2022",
            "2021",
            "2020",
            "2019",
            "Earlier"
        )
        setupSpinner(binding.graduationYearSpinner, graduationYears)

        // Purpose Setup
        val purposes = arrayOf(
            "Please Select",
            "Employment Verification",
            "Further Studies",
            "Professional Registration",
            "International Recognition",
            "Lost Original Document",
            "Other"
        )
        setupSpinner(binding.purposeSpinner, purposes)
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
            documentTypeSpinner.isEnabled = enabled
            graduationYearSpinner.isEnabled = enabled
            purposeSpinner.isEnabled = enabled

            // Input fields
            graduationDateInput.isEnabled = enabled
            studentNumberInput.isEnabled = enabled
            qualificationDetailsInput.isEnabled = enabled
            additionalDetailsInput.isEnabled = enabled

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
                            studentNumberInput.setText(enteredId)
                            qualificationDetailsInput.setText(document.getString("qualification") ?: "")
                        }
                        setInputsEnabled(true)
                        Toast.makeText(context, "Student details loaded successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("GraduationConfirmationFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("GraduationConfirmationFragment", "Error fetching document: ${e.message}", e)
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
            val documentType = (documentTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val graduationYear = (graduationYearSpinner as? AutoCompleteTextView)?.text.toString()
            val purpose = (purposeSpinner as? AutoCompleteTextView)?.text.toString()
            val graduationDate = graduationDateInput.text.toString()
            val studentNumber = studentNumberInput.text.toString()
            val qualificationDetails = qualificationDetailsInput.text.toString()
            val additionalDetails = additionalDetailsInput.text.toString()

            when {
                studentId.isEmpty() -> {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
                documentType == "Please Select" -> {
                    Toast.makeText(context, "Please select the document type required", Toast.LENGTH_SHORT).show()
                }
                graduationYear == "Please Select" -> {
                    Toast.makeText(context, "Please select your graduation year", Toast.LENGTH_SHORT).show()
                }
                purpose == "Please Select" -> {
                    Toast.makeText(context, "Please select the purpose of this request", Toast.LENGTH_SHORT).show()
                }
                graduationDate.isEmpty() -> {
                    Toast.makeText(context, "Please confirm your graduation date", Toast.LENGTH_SHORT).show()
                }
                studentNumber.isEmpty() -> {
                    Toast.makeText(context, "Please enter your student number from graduation", Toast.LENGTH_SHORT).show()
                }
                qualificationDetails.isEmpty() -> {
                    Toast.makeText(context, "Please provide your qualification details", Toast.LENGTH_SHORT).show()
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
                        "documentType" to documentType,
                        "graduationYear" to graduationYear,
                        "purpose" to purpose,
                        "graduationDate" to graduationDate,
                        "studentNumber" to studentNumber,
                        "qualificationDetails" to qualificationDetails,
                        "additionalDetails" to additionalDetails,
                        "supportingDocs" to selectedDocsFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "Alumni Graduation Document Request"),
                        "queryCategory" to "Alumni Student Queries",
                        "queryType" to "Alumni Graduation Confirmation",
                        "status" to "Pending",
                        "dateSubmitted" to FieldValue.serverTimestamp()
                    )

                    db.collection("AlumniQuery")
                        .add(queryData)
                        .addOnSuccessListener { documentReference ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Log.d("SubmitQuery", "Query submitted with ID: ${documentReference.id}")
                            Toast.makeText(context, "Graduation confirmation submitted successfully!", Toast.LENGTH_LONG).show()
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
            graduationDateInput.setText("")
            studentNumberInput.setText("")
            qualificationDetailsInput.setText("")
            additionalDetailsInput.setText("")

            // Reset spinners
            (documentTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (graduationYearSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (purposeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)

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