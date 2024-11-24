package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FAQFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_f_a_q, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Set up click listeners for each option
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        // Log Query
        view.findViewById<View>(R.id.logQueryLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.tutorial_log_query)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                Log.e("FAQFragment", "Error showing video: ${e.message}")
                Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
            }
        }

        // Change Language
        view.findViewById<View>(R.id.changeLanguageLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.tutorial_language)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                Log.e("FAQFragment", "Error showing video: ${e.message}")
                Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
            }
        }

        // Send Messages
        view.findViewById<View>(R.id.sendMessagesLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.tutorial_messages)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                Log.e("FAQFragment", "Error showing video: ${e.message}")
                Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
            }
        }

        // Rate Service
        view.findViewById<View>(R.id.rateServiceLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.tutorial_rate_service)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                Log.e("FAQFragment", "Error showing video: ${e.message}")
                Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
            }
        }

        // View Modules
        view.findViewById<View>(R.id.viewModulesLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.tutorial_view_modules)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                Log.e("FAQFragment", "Error showing video: ${e.message}")
                Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
            }
        }

        // View Pending Queries
        view.findViewById<View>(R.id.viewPendingQueriesLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.tutorial_pending)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                Log.e("FAQFragment", "Error showing video: ${e.message}")
                Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
            }
        }

        // View Solved Queries
        view.findViewById<View>(R.id.viewSolvedQueriesLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.tutorial_solved)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                Log.e("FAQFragment", "Error showing video: ${e.message}")
                Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
            }
        }

        // View Processing Queries
        view.findViewById<View>(R.id.viewProcessingQueriesLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.tutorial_processing)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                Log.e("FAQFragment", "Error showing video: ${e.message}")
                Toast.makeText(context, "Error playing video", Toast.LENGTH_SHORT).show()
            }
        }
    }

}