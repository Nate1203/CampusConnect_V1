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
import za.co.varstycollege.st1009749.campusconnect__v1.databinding.FragmentStudentSystemsQueryBinding

class StudentSystemsQueryFragment : Fragment() {
    private var _binding: FragmentStudentSystemsQueryBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var selectedFileName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentSystemsQueryBinding.inflate(inflater, container, false)
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
            "Login Issues",
            "Password Reset",
            "System Access Problem",
            "Technical Error",
            "Student Portal Issues",
            "Mobile App Issues",
            "Browser Compatibility",
            "Connection Problems",
            "System Navigation Help"
        )
        setupSpinner(binding.queryTypeSpinner, queryTypes)

        // System/Platform Setup
        val systems = arrayOf(
            "Please Select",
            "Student Portal",
            "Mobile App",
            "Online Library",
            "Learning Management System",
            "Student Email",
            "Academic Records System",
            "Assessment Portal",
            "Registration System"
        )
        setupSpinner(binding.systemTypeSpinner, systems)

        // Device Type Setup
        val deviceTypes = arrayOf(
            "Please Select",
            "Desktop Computer",
            "Laptop",
            "Tablet",
            "Mobile Phone",
            "Other"
        )
        setupSpinner(binding.deviceTypeSpinner, deviceTypes)

        // Browser Type Setup
        val browserTypes = arrayOf(
            "Please Select",
            "Google Chrome",
            "Mozilla Firefox",
            "Microsoft Edge",
            "Safari",
            "Opera",
            "Other"
        )
        setupSpinner(binding.browserTypeSpinner, browserTypes)

        // Error Frequency Setup
        val frequencies = arrayOf(
            "Please Select",
            "Once only",
            "Intermittent",
            "Frequent",
            "Constant",
            "On specific action"
        )
        setupSpinner(binding.errorFrequencySpinner, frequencies)
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
            systemTypeSpinner.isEnabled = enabled
            deviceTypeSpinner.isEnabled = enabled
            browserTypeSpinner.isEnabled = enabled
            errorFrequencySpinner.isEnabled = enabled

            // Input fields
            errorMessageInput.isEnabled = enabled
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
                        // Set basic info
                        binding.apply {
                            nameInput.setText(document.getString("name") ?: "")
                            surnameInput.setText(document.getString("surname") ?: "")
                            qualificationInput.setText(document.getString("qualification") ?: "")
                        }

                        setInputsEnabled(true)
                        Toast.makeText(context, "Student details loaded successfully", Toast.LENGTH_SHORT).show()

                    } catch (e: Exception) {
                        Log.e("StudentSystemsQueryFragment", "Error parsing data: ${e.message}", e)
                        Toast.makeText(context, "Error loading student data", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } else {
                    Toast.makeText(context, "Student ID not found", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            }
            .addOnFailureListener { e ->
                Log.e("StudentSystemsQueryFragment", "Error fetching document: ${e.message}", e)
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
            val systemType = (systemTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val deviceType = (deviceTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val browserType = (browserTypeSpinner as? AutoCompleteTextView)?.text.toString()
            val errorFrequency = (errorFrequencySpinner as? AutoCompleteTextView)?.text.toString()
            val errorMessage = errorMessageInput.text.toString()
            val description = descriptionInput.text.toString()

            when {
                studentId.isEmpty() -> {
                    Toast.makeText(context, "Please fetch student details first", Toast.LENGTH_SHORT).show()
                }
                queryType == "Please Select" -> {
                    Toast.makeText(context, "Please select a query type", Toast.LENGTH_SHORT).show()
                }
                systemType == "Please Select" -> {
                    Toast.makeText(context, "Please select a system/platform", Toast.LENGTH_SHORT).show()
                }
                deviceType == "Please Select" -> {
                    Toast.makeText(context, "Please select a device type", Toast.LENGTH_SHORT).show()
                }
                browserType == "Please Select" -> {
                    Toast.makeText(context, "Please select a browser", Toast.LENGTH_SHORT).show()
                }
                errorFrequency == "Please Select" -> {
                    Toast.makeText(context, "Please select an error frequency", Toast.LENGTH_SHORT).show()
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
                        "systemType" to systemType,
                        "deviceType" to deviceType,
                        "browserType" to browserType,
                        "errorFrequency" to errorFrequency,
                        "errorMessage" to errorMessage,
                        "description" to description,
                        "attachedFile" to selectedFileName,
                        "queryName" to (arguments?.getString("queryTitle") ?: "Student Systems Query"),
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
            errorMessageInput.setText("")
            descriptionInput.setText("")

            // Reset spinners
            (queryTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (systemTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (deviceTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (browserTypeSpinner as? AutoCompleteTextView)?.setText("Please Select", false)
            (errorFrequencySpinner as? AutoCompleteTextView)?.setText("Please Select", false)
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

        fun newInstance(queryTitle: String) = StudentSystemsQueryFragment().apply {
            arguments = Bundle().apply {
                putString("queryTitle", queryTitle)
            }
        }
    }
}