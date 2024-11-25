package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LocaleHelper.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        FirebaseStorage.getInstance()

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_background)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val languageCode = LocaleHelper.getLanguage(this)
        LocaleHelper.setLocale(this, languageCode)

        if (isLoggedIn()) {
            restoreUserSession()
        } else {
            setupLoginUI()
        }
    }


    // In MainActivity's updateNavHeaderImage()
    fun updateNavHeaderImage() {
        try {
            val imageFile = File(filesDir, "admin_profile_image.jpg")
            Log.d("MainActivity", "Updating nav header image. File exists: ${imageFile.exists()}")
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                val navigationView = findViewById<NavigationView>(R.id.admin_nav_view)
                val headerView = navigationView.getHeaderView(0)
                val navHeaderImage = headerView.findViewById<CircleImageView>(R.id.nav_header_image)
                navHeaderImage.setImageBitmap(bitmap)
                Log.d("MainActivity", "Nav header image updated successfully")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating nav header: ${e.message}")
            e.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("CampusConnectPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("nav_header_image_updated", false)) {
            updateNavHeaderImage()
            prefs.edit().putBoolean("nav_header_image_updated", false).apply()
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val languageCode = LocaleHelper.getLanguage(this)
        LocaleHelper.setLocale(this, languageCode)
    }

    private fun setupLoginUI() {
        val adapter = LoginPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Student"
                1 -> "Admin"
                else -> null
            }
        }.attach()
    }

    private fun isLoggedIn(): Boolean {
        val sharedPref = getSharedPreferences("CampusConnectPrefs", MODE_PRIVATE)
        val loginToken = sharedPref.getString("loginToken", null)
        val studentEmail = sharedPref.getString("studentEmail", null)
        val adminEmail = sharedPref.getString("adminEmail", null)

        return loginToken != null && (studentEmail != null || adminEmail != null)
    }

    private fun restoreUserSession() {
        val sharedPref = getSharedPreferences("CampusConnectPrefs", MODE_PRIVATE)
        val isAdmin = sharedPref.getBoolean("isAdmin", false)

        if (isAdmin) {
            showDashboard(AdminDashboardFragment())
        } else {
            showDashboard(StudentDashboardFragment())
        }
    }

    fun showLoginPage(isAdmin: Boolean) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        viewPager.visibility = View.VISIBLE
        tabLayout.visibility = View.VISIBLE
        viewPager.currentItem = if (isAdmin) 1 else 0
    }

    fun showSignupPage(fragment: Fragment) {
        viewPager.visibility = View.GONE
        tabLayout.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun hideLoginUI() {
        viewPager.visibility = View.GONE
        tabLayout.visibility = View.GONE
    }

    fun showDashboard(fragment: Fragment) {
        hideLoginUI()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private inner class LoginPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> StudentLoginFragment()
                1 -> AdminLoginFragment()
                else -> throw IllegalStateException("Invalid position $position")
            }
        }
    }
}