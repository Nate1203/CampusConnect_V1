package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ViewModulesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var moduleAdapter: ModuleAdapter
    private lateinit var noModulesTextView: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_view_modules, container, false)

        db = FirebaseFirestore.getInstance()
        recyclerView = view.findViewById(R.id.modulesRecyclerView)
        noModulesTextView = view.findViewById(R.id.noModulesTextView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        fetchStudentIdAndModules()

        return view
    }

    private fun fetchStudentIdAndModules() {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val email = sharedPref?.getString("studentEmail", "") ?: ""

        Log.d("ViewModulesFragment", "Fetching student ID for email: $email")

        if (email.isNotEmpty()) {
            db.collection("students")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val student = documents.documents[0]
                        val studentId = student.getString("studentId") ?: ""
                        Log.d("ViewModulesFragment", "Found student ID: $studentId")
                        fetchStudentModules(studentId)
                    } else {
                        Log.d("ViewModulesFragment", "No student found for email: $email")
                        showNoModulesMessage("No student found for this email")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ViewModulesFragment", "Error fetching student ID", e)
                    showNoModulesMessage("Error fetching student information: ${e.message}")
                }
        } else {
            Log.e("ViewModulesFragment", "No email found in SharedPreferences")
            showNoModulesMessage("No user email found")
        }
    }

    private fun fetchStudentModules(studentId: String) {
        db.collection("StudentModules").document(studentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val modules = document.get("modules") as? List<Map<String, String>> ?: listOf()
                    Log.d("ViewModulesFragment", "Fetched modules: $modules")
                    if (modules.isNotEmpty()) {
                        moduleAdapter = ModuleAdapter(modules)
                        recyclerView.adapter = moduleAdapter
                        recyclerView.visibility = View.VISIBLE
                        noModulesTextView.visibility = View.GONE
                    } else {
                        showNoModulesMessage("No modules found for this student")
                    }
                } else {
                    Log.d("ViewModulesFragment", "No modules document found for student ID: $studentId")
                    showNoModulesMessage("No modules found for this student")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ViewModulesFragment", "Error fetching modules", e)
                showNoModulesMessage("Error fetching modules: ${e.message}")
            }
    }

    private fun showNoModulesMessage(message: String) {
        recyclerView.visibility = View.GONE
        noModulesTextView.visibility = View.VISIBLE
        noModulesTextView.text = message
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private inner class ModuleAdapter(private val modules: List<Map<String, String>>) :
        RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

        inner class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val codeTextView: TextView = itemView.findViewById(R.id.moduleCodeTextView)
            val nameTextView: TextView = itemView.findViewById(R.id.moduleNameTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_module, parent, false)
            return ModuleViewHolder(view)
        }

        override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
            val module = modules[position]
            holder.codeTextView.text = module["code"]
            holder.nameTextView.text = module["name"]
        }

        override fun getItemCount() = modules.size
    }
}