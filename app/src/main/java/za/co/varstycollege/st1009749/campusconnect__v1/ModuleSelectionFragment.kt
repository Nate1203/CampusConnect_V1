package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class ModuleSelectionFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var studentId: String
    private lateinit var name: String
    private lateinit var surname: String
    private lateinit var qualification: String
    private lateinit var moduleContainer: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_module_selection, container, false)

        db = FirebaseFirestore.getInstance()
        studentId = arguments?.getString("studentId") ?: ""
        name = arguments?.getString("name") ?: ""
        surname = arguments?.getString("surname") ?: ""
        qualification = arguments?.getString("qualification") ?: ""

        moduleContainer = view.findViewById(R.id.moduleContainer)
        val saveButton: Button = view.findViewById(R.id.saveModulesButton)

        fetchAndDisplayModules()

        saveButton.setOnClickListener { saveSelectedModules() }

        return view
    }

    private fun fetchAndDisplayModules() {
        db.collection("qualifications").document(qualification)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val modules = document.data as? Map<String, String> ?: emptyMap()
                    displayModules(modules)
                } else {
                    Toast.makeText(context, "No modules found for this qualification", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching modules: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayModules(modules: Map<String, String>) {
        moduleContainer.removeAllViews()
        modules.forEach { (code, name) ->
            val checkBox = CheckBox(context).apply {
                text = "$code - $name"
                tag = Module(code, name)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                textSize = 18f
                setPadding(0, 16, 0, 16)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
            }
            moduleContainer.addView(checkBox)
        }
    }

    private fun saveSelectedModules() {
        val selectedModules = mutableListOf<Module>()
        for (i in 0 until moduleContainer.childCount) {
            val view = moduleContainer.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                selectedModules.add(view.tag as Module)
            }
        }

        if (selectedModules.isEmpty()) {
            Toast.makeText(context, "Please select at least one module", Toast.LENGTH_SHORT).show()
            return
        }

        val moduleData = hashMapOf(
            "studentId" to studentId,
            "name" to name,
            "surname" to surname,
            "qualification" to qualification,
            "modules" to selectedModules.map { mapOf("code" to it.code, "name" to it.name) }
        )

        db.collection("StudentModules")
            .document(studentId)
            .set(moduleData)
            .addOnSuccessListener {
                Toast.makeText(context, "Modules saved successfully", Toast.LENGTH_SHORT).show()
                navigateToLoginPage()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving modules: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToLoginPage() {
        val loginFragment = StudentLoginFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, loginFragment)
            .addToBackStack(null)
            .commit()
    }

    data class Module(val code: String, val name: String)

    companion object {
        fun newInstance(studentId: String, name: String, surname: String, qualification: String) =
            ModuleSelectionFragment().apply {
                arguments = Bundle().apply {
                    putString("studentId", studentId)
                    putString("name", name)
                    putString("surname", surname)
                    putString("qualification", qualification)
                }
            }
    }
}