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
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentTranscriptRequestBinding
import java.text.SimpleDateFormat
import java.util.*

class TranscriptRequestFragment : Fragment() {
    private var _binding: FragmentTranscriptRequestBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedDocsFileName: String = ""

    companion object {
        const val FILE_PICKER_REQUEST_CODE = 123

        fun newInstance(queryTitle: String) = TranscriptRequestFragment().apply {
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
        _binding = FragmentTranscriptRequestBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupSpinners()
        setupButtons()
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


    private fun setupSpinners() {
        // Transcript Type Setup
        val transcriptTypes = arrayOf(
            "Please Select",
            "Official Academic Transcript",
            "Academic Record",
            "Detailed Progress Report",
            "Subject Details",
            "Full Academic History"
        )
        setupSpinner(binding.transcriptTypeSpinner, transcriptTypes)

        // Delivery Method Setup
        val deliveryMethods = arrayOf(
            "Please Select",
            "Digital Copy (Email)",
            "Collection from Campus",
            "Courier Delivery",
            "Registered Mail"
        )
        setupSpinner(binding.deliveryMethodSpinner, deliveryMethods)

        // Purpose Setup
        val purposes = arrayOf(
            "Please Select",
            "Further Studies",
            "Employment",
            "Professional Registration",
            "Immigration",
            "Personal Records",
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
            transcriptTypeSpinner.isEnabled = enabled
            deliveryMethodSpinner.isEnabled = enabled
            purposeSpinner.isEnabled = enabled

            // Input fields
            deliveryAddressInput.isEnabled = enabled
            destinationDetailsInput.isEnabled = enabled
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
                         }
                        setInputsEnabled(true)
                        Toast.makeText(context, "Student details loaded successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("TranscriptRequestFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("TranscriptRequestFragment", "Error fetching document: ${e.message}", e)
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
            val transcriptType = (transcriptTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val deliveryMethod = (deliveryMethodSpinner as? AutoCompleteTextView)?.text.toString()
            val purpose = (purposeSpinner as? AutoCompleteTextView)?.text.toString()
            val deliveryAddress = deliveryAddressInput.text.toString()
            val destinationDetails = destinationDetailsInput.text.toString()
            val additionalDetails = additionalDetailsInput.text.toString()

            when {
                studentId.isEmpty() -> {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
                transcriptType == "Please Select" -> {
                    Toast.makeText(context, "Please select transcript type", Toast.LENGTH_SHORT).show()
                }
                deliveryMethod == "Please Select" -> {
                    Toast.makeText(context, "Please select delivery method", Toast.LENGTH_SHORT).show()
                }
                purpose == "Please Select" -> {
                    Toast.makeText(context, "Please select purpose for transcript", Toast.LENGTH_SHORT).show()
                }
                deliveryAddress.isEmpty() && (deliveryMethod == "Courier Delivery" ||
                        deliveryMethod == "Registered Mail") -> {
                    Toast.makeText(context, "Please provide delivery address", Toast.LENGTH_SHORT).show()
                }
                destinationDetails.isEmpty() && purpose != "Personal Records" -> {
                    Toast.makeText(context, "Please provide destination details", Toast.LENGTH_SHORT).show()
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
                        "transcriptType" to transcriptType,
                        "deliveryMethod" to deliveryMethod,
                        "purpose" to purpose,
                        "deliveryAddress" to deliveryAddress,
                        "destinationDetails" to destinationDetails,
                        "additionalDetails" to additionalDetails,
                        "supportingDocs" to selectedDocsFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "Alumni Transcript Request"),
                        "queryCategory" to "Alumni Student Queries",
                        "queryType" to "Alumni Transcript Request",
                        "status" to "Pending",
                        "dateSubmitted" to FieldValue.serverTimestamp()
                    )

                    db.collection("AlumniQuery")
                        .add(queryData)
                        .addOnSuccessListener { documentReference ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Log.d("SubmitQuery", "Query submitted with ID: ${documentReference.id}")
                            Toast.makeText(context, "Transcript request submitted successfully!", Toast.LENGTH_LONG).show()
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
            deliveryAddressInput.setText("")
            destinationDetailsInput.setText("")
            additionalDetailsInput.setText("")

            // Reset spinners
            (transcriptTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (deliveryMethodSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
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