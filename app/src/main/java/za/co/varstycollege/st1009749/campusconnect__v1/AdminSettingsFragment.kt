package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.messaging.FirebaseMessaging

class AdminSettingsFragment : Fragment() {
    private lateinit var languageGroup: RadioGroup
    private lateinit var notificationSwitch: SwitchMaterial
    private lateinit var soundSwitch: SwitchMaterial
    private lateinit var queryAlertSwitch: SwitchMaterial
    private lateinit var darkModeSwitch: SwitchMaterial
    private lateinit var textSizeSeekBar: SeekBar
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        loadSavedSettings()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        languageGroup = view.findViewById(R.id.languageGroup)
        notificationSwitch = view.findViewById(R.id.notificationSwitch)
        soundSwitch = view.findViewById(R.id.soundSwitch)
        queryAlertSwitch = view.findViewById(R.id.queryAlertSwitch)
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch)
        textSizeSeekBar = view.findViewById(R.id.textSizeSeekBar)
        saveButton = view.findViewById(R.id.saveSettingsButton)
    }

    private fun loadSavedSettings() {
        val sharedPref = requireActivity().getSharedPreferences("AdminSettings", Context.MODE_PRIVATE)

        // Load language setting
        when (LocaleHelper.getLanguage(requireContext())) {
            "en" -> languageGroup.check(R.id.englishRadio)
            "af" -> languageGroup.check(R.id.afrikaansRadio)
            "zu" -> languageGroup.check(R.id.zuluRadio)
        }

        // Load notification settings
        notificationSwitch.isChecked = sharedPref.getBoolean("notifications_enabled", true)
        soundSwitch.isChecked = sharedPref.getBoolean("notification_sound", true)
        queryAlertSwitch.isChecked = sharedPref.getBoolean("query_alerts", true)

        // Load display settings
        darkModeSwitch.isChecked = sharedPref.getBoolean("dark_mode", false)
        textSizeSeekBar.progress = sharedPref.getInt("text_size", 50)

        // Update dependent controls
        soundSwitch.isEnabled = notificationSwitch.isChecked
        queryAlertSwitch.isEnabled = notificationSwitch.isChecked
    }

    private fun setupListeners() {
        saveButton.setOnClickListener {
            saveSettings()
        }

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            soundSwitch.isEnabled = isChecked
            queryAlertSwitch.isEnabled = isChecked
            updateNotificationSettings(isChecked)
        }
    }

    private fun updateNotificationSettings(enabled: Boolean) {
        if (enabled) {
            // Subscribe to FCM topics for admin notifications
            FirebaseMessaging.getInstance().subscribeToTopic("admin_notifications")
            FirebaseMessaging.getInstance().subscribeToTopic("new_queries")
        } else {
            // Unsubscribe from FCM topics
            FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_notifications")
            FirebaseMessaging.getInstance().unsubscribeFromTopic("new_queries")
        }
    }

    private fun saveSettings() {
        val sharedPref = requireActivity().getSharedPreferences("AdminSettings", Context.MODE_PRIVATE)
        val currentLanguage = LocaleHelper.getLanguage(requireContext())

        val newLanguageCode = when (languageGroup.checkedRadioButtonId) {
            R.id.englishRadio -> "en"
            R.id.afrikaansRadio -> "af"
            R.id.zuluRadio -> "zu"
            else -> "en"
        }

        with(sharedPref.edit()) {
            // Save language setting
            putString("language", newLanguageCode)

            // Save notification settings
            putBoolean("notifications_enabled", notificationSwitch.isChecked)
            putBoolean("notification_sound", soundSwitch.isChecked)
            putBoolean("query_alerts", queryAlertSwitch.isChecked)

            // Save display settings
            putBoolean("dark_mode", darkModeSwitch.isChecked)
            putInt("text_size", textSizeSeekBar.progress)
            apply()
        }

        // Show save confirmation
        Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show()

        // If language changed, restart the activity
        if (currentLanguage != newLanguageCode) {
            LocaleHelper.setLocale(requireContext(), newLanguageCode)
            restartApp()
        }
    }

    private fun restartApp() {
        activity?.let { activity ->
            val intent = Intent(activity, activity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            activity.finish()
        }
    }
}