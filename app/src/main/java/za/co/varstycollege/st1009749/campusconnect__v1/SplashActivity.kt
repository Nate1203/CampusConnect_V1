package za.co.varstycollege.st1009749.campusconnect__v1

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Handler
import android.os.Looper

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splashText = findViewById<FillableTextView>(R.id.splash_text)

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1700
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            splashText.setFillProgress(progress)
        }

        animator.start()

        // Navigate to MainActivity after animation ends
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2350) // Match this with the animation duration
    }
}