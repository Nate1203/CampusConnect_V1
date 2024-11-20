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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentFinanceQueryBinding

class FinanceQueryFragment : Fragment() {
    private var _binding: FragmentFinanceQueryBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedFileName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinanceQueryBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupSpinners()
        setupButtons()
        setInputsEnabled(false)

        return binding.root
    }

    private fun initializeViews() {
        // Set title from arguments
        arguments?.getString("queryTitle")?.let { title ->
            binding.titleText.apply {
                text = title
                setTextColor(ContextCompat.getColor(requireContext(), R.color.electric_blue))
            }
        }
    }

    private fun setupSpinners() {
        // Query Type Setup
        val queryTypes = arrayOf(
            "Please Select",
            "Missing Payment",
            "Statement Query",
            "Refund Query",
            "Payment Plan Request",
            "NSFAS Query",
            "Bursary Query",
            "Study Loan Query",
            "Fee Structure Query",
            "General Finance Query"
        )
        val queryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, queryTypes)
        (binding.queryTypeSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(queryAdapter)
            setText(queryTypes[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Payment Type Setup
        val paymentTypes = arrayOf(
            "Please Select",
            "EFT Payment",
            "Credit Card Payment",
            "Cash Deposit",
            "NSFAS",
            "Bursary",
            "Study Loan",
            "Corporate Sponsor"
        )
        val paymentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, paymentTypes)
        (binding.paymentTypeSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(paymentAdapter)
            setText(paymentTypes[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Statement Period Setup
        val statementPeriods = arrayOf(
            "Please Select",
            "January 2024",
            "February 2024",
            "March 2024",
            "April 2024",
            "May 2024",
            "June 2024",
            "July 2024",
            "August 2024",
            "September 2024",
            "October 2024",
            "November 2024",
            "December 2024",
            "Full Year 2024"
        )
        val periodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statementPeriods)
        (binding.statementPeriodSpinner as? AutoCompleteTextView)?.apply {
            setAdapter(periodAdapter)
            setText(statementPeriods[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Fee Category Setup
        val feeCategories = arrayOf(
            "Please Select",
            "Registration Fee",
            "Tuition Fee",
            "Exam Fee",
            "Study Material Fee",
            "Additional Charges",
            "Late Payment Penalties"
        )
        val feeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, feeCategories)
        (binding.feeCategorySpinner as? AutoCompleteTextView)?.apply {
            setAdapter(feeAdapter)
            setText(feeCategories[0], false)
            setOnClickListener {
                if (!isEnabled) {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButtons() {
        binding.fetchDetailsButton.setOnClickListener {
            val studentId = binding.studentIdInput.text.toString()
            if (studentId.isNotEmpty()) {
                fetchStudentDetails()
            } else {
                Toast.makeText(context, "Please enter a Student ID", Toast.LENGTH_SHORT).show()
            }
        }

        binding.uploadButton.setOnClickListener {
            if (!it.isEnabled) {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
            } else {
                selectFile()
            }
        }

        binding.submitButton.setOnClickListener {
            if (!it.isEnabled) {
                Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
            } else {
                submitForm()
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.apply {
            // Spinners
            queryTypeSpinner.isEnabled = enabled
            paymentTypeSpinner.isEnabled = enabled
            statementPeriodSpinner.isEnabled = enabled
            feeCategorySpinner.isEnabled = enabled

            // Input fields
            paymentReferenceInput.isEnabled = enabled
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

    private fun submitForm() {
        with(binding) {
            val studentId = studentIdInput.text.toString()
            val queryType = (queryTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val paymentType = (paymentTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val statementPeriod = (statementPeriodSpinner as? AutoCompleteTextView)?.text.toString()
            val feeCategory = (feeCategorySpinner as? AutoCompleteTextView)?.text.toString()
            val paymentReference = paymentReferenceInput.text.toString()
            val description = descriptionInput.text.toString()

            when {
                studentId.isEmpty() -> {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
                queryType == "Please Select" -> {
                    Toast.makeText(context, "Please select a query type", Toast.LENGTH_SHORT).show()
                }
                paymentType == "Please Select" -> {
                    Toast.makeText(context, "Please select a payment type", Toast.LENGTH_SHORT).show()
                }
                statementPeriod == "Please Select" -> {
                    Toast.makeText(context, "Please select a statement period", Toast.LENGTH_SHORT).show()
                }
                feeCategory == "Please Select" -> {
                    Toast.makeText(context, "Please select a fee category", Toast.LENGTH_SHORT).show()
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
                        "paymentType" to paymentType,
                        "statementPeriod" to statementPeriod,
                        "feeCategory" to feeCategory,
                        "paymentReference" to paymentReference,
                        "description" to description,
                        "attachedFile" to selectedFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "Finance Query"),
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


    private fun fetchStudentDetails() {
        val enteredId = binding.studentIdInput.text.toString()
        if (enteredId.isEmpty()) {
            Toast.makeText(context, "Please enter a Student ID", Toast.LENGTH_SHORT).show()
            return
        }

        binding.fetchDetailsButton.isEnabled = false

        db.collection("students")
            .whereEqualTo("studentId", enteredId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "No student found with this ID", Toast.LENGTH_SHORT).show()
                    clearFields()
                } else {
                    val student = documents.documents[0]
                    binding.nameInput.setText(student.getString("firstName"))
                    binding.surnameInput.setText(student.getString("lastName"))

                    fetchRegistrationDetails(enteredId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching student details: ${e.message}", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .addOnCompleteListener {
                binding.fetchDetailsButton.isEnabled = true
            }
    }

    private fun fetchRegistrationDetails(studentId: String) {
        db.collection("StudentModules")
            .document(studentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    binding.qualificationInput.setText(document.getString("qualification") ?: "")
                    setInputsEnabled(true)
                    Toast.makeText(context, "Student details loaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Student registration not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching registration details", Toast.LENGTH_SHORT).show()
                clearFields()
            }
    }

    private fun clearFields() {
        binding.apply {
            nameInput.setText("")
            surnameInput.setText("")
            qualificationInput.setText("")
            paymentReferenceInput.setText("")
            descriptionInput.setText("")

            // Reset spinners
            (queryTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (paymentTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (statementPeriodSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (feeCategorySpinner as? AutoCompleteTextView)?.setText("Please Select", false)
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

        fun newInstance(queryTitle: String) = FinanceQueryFragment().apply {
            arguments = Bundle().apply {
                putString("queryTitle", queryTitle)
            }
        }
    }
}