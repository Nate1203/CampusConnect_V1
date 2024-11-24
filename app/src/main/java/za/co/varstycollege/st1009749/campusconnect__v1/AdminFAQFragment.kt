package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class AdminFAQFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_f_a_q, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        // Solve Queries
        view.findViewById<View>(R.id.solveQueriesLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.solve_queries_tutorial)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                showToast("Tutorial video coming soon")
            }
        }

        // View Analytics
        view.findViewById<View>(R.id.viewAnalyticsLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.analytics_tutorial)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                showToast("Tutorial video coming soon")
            }
        }

        // Manage Students
        view.findViewById<View>(R.id.manageStudentsLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.manage_students_chats)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                showToast("Tutorial video coming soon")
            }
        }

        // View Leaderboard
        view.findViewById<View>(R.id.viewLeaderboardLayout).setOnClickListener {
            try {
                VideoDialogFragment.newInstance(R.raw.leaderboard_tutorial)
                    .show(parentFragmentManager, "VideoDialog")
            } catch (e: Exception) {
                showToast("Tutorial video coming soon")
            }
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}