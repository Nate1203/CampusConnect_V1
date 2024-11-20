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
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentUncollectedCertificatesBinding
import java.text.SimpleDateFormat
import java.util.*

class UncollectedCertificatesFragment : Fragment() {
    private var _binding: FragmentUncollectedCertificatesBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedDocsFileName: String = ""

    companion object {
        const val FILE_PICKER_REQUEST_CODE = 123

        fun newInstance(queryTitle: String) = UncollectedCertificatesFragment().apply {
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
        _binding = FragmentUncollectedCertificatesBinding.inflate(inflater, container, false)
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
        binding.preferredCollectionDateInput.setOnClickListener {
            if (!it.isEnabled) {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val materialDateBuilder = MaterialDatePicker.Builder.datePicker().apply {
                setTitleText("Select Preferred Collection Date")
                setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            }

            val datePicker = materialDateBuilder.build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.preferredCollectionDateInput.setText(dateFormat.format(calendar.time))
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupSpinners() {
        // Collection Method Setup
        val collectionMethods = arrayOf(
            "Please Select",
            "Personal Collection",
            "Courier Delivery",
            "International Shipping"
        )
        setupSpinner(binding.collectionMethodSpinner, collectionMethods)

        // Campus Setup
        val campusLocations = arrayOf(
            "Please Select",
            "Pretoria Campus",
            "Johannesburg Campus",
            "Cape Town Campus",
            "Durban Campus"
        )
        setupSpinner(binding.campusLocationSpinner, campusLocations)

        // Document Type Setup
        val documentTypes = arrayOf(
            "Please Select",
            "Original Certificate",
            "Academic Record",
            "Complete Package"
        )
        setupSpinner(binding.documentTypeSpinner, documentTypes)
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
            collectionMethodSpinner.isEnabled = enabled
            campusLocationSpinner.isEnabled = enabled
            documentTypeSpinner.isEnabled = enabled

            // Input fields
            contactNumberInput.isEnabled = enabled
            preferredCollectionDateInput.isEnabled = enabled
            deliveryAddressInput.isEnabled = enabled
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
                            contactNumberInput.setText(document.getString("contactNumber") ?: "")
                        }
                        setInputsEnabled(true)
                        Toast.makeText(context, "Student details loaded successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("UncollectedCertificatesFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UncollectedCertificatesFragment", "Error fetching document: ${e.message}", e)
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
            val collectionMethod = (collectionMethodSpinner as? AutoCompleteTextView)?.text.toString()
            val campusLocation = (campusLocationSpinner as? AutoCompleteTextView)?.text.toString()
            val documentType = (documentTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val contactNumber = contactNumberInput.text.toString()
            val preferredCollectionDate = preferredCollectionDateInput.text.toString()
            val deliveryAddress = deliveryAddressInput.text.toString()
            val additionalDetails = additionalDetailsInput.text.toString()

            when {
                studentId.isEmpty() -> {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
                collectionMethod == "Please Select" -> {
                    Toast.makeText(context, "Please select collection method", Toast.LENGTH_SHORT).show()
                }
                campusLocation == "Please Select" -> {
                    Toast.makeText(context, "Please select campus location", Toast.LENGTH_SHORT).show()
                }
                documentType == "Please Select" -> {
                    Toast.makeText(context, "Please select document type", Toast.LENGTH_SHORT).show()
                }
                contactNumber.isEmpty() -> {
                    Toast.makeText(context, "Please provide contact number", Toast.LENGTH_SHORT).show()
                }
                (collectionMethod == "Courier Delivery" || collectionMethod == "International Shipping")
                        && deliveryAddress.isEmpty() -> {
                    Toast.makeText(context, "Please provide delivery address", Toast.LENGTH_SHORT).show()
                }
                preferredCollectionDate.isEmpty() && collectionMethod == "Personal Collection" -> {
                    Toast.makeText(context, "Please enter preferred collection date", Toast.LENGTH_SHORT).show()
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
                        "collectionMethod" to collectionMethod,
                        "campusLocation" to campusLocation,
                        "documentType" to documentType,
                        "contactNumber" to contactNumber,
                        "preferredCollectionDate" to preferredCollectionDate,
                        "deliveryAddress" to deliveryAddress,
                        "additionalDetails" to additionalDetails,
                        "supportingDocs" to selectedDocsFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "Alumni Uncollected Certificates Request"),
                        "queryCategory" to "Alumni Student Queries",
                        "queryType" to "Alumni Uncollected Certificates",
                        "status" to "Pending",
                        "dateSubmitted" to FieldValue.serverTimestamp()
                    )

                    db.collection("AlumniQuery")
                        .add(queryData)
                        .addOnSuccessListener { documentReference ->
                            progressDialog.dismiss()
                            submitButton.isEnabled = true
                            Log.d("SubmitQuery", "Query submitted with ID: ${documentReference.id}")
                            Toast.makeText(context, "Uncollected certificates request submitted successfully!", Toast.LENGTH_LONG).show()
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
            contactNumberInput.setText("")
            preferredCollectionDateInput.setText("")
            deliveryAddressInput.setText("")
            additionalDetailsInput.setText("")

            // Reset spinners
            (collectionMethodSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (campusLocationSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (documentTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)

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