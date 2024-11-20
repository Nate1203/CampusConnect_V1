package za.co.varstycollege.st1009749.campusconnect__v1

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.fragment.app.Fragment
import java.util.*

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    fun updateResources(fragment: Fragment, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = fragment.requireContext().resources
        val config = Configuration(resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            config.locale = locale
        }
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"
    }

    fun getLanguageName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "af" -> "Afrikaans"
            "zu" -> "isiZulu"
            else -> "English"
        }
    }
}