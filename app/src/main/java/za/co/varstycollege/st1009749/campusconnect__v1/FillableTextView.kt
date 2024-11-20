package za.co.varstycollege.st1009749.campusconnect__v1

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat

class FillableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var fillProgress = 0f
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradient = LinearGradient(0f, 0f, 1f, 0f, intArrayOf(
        Color.parseColor("#7DF9FF"),  // Your electric blue color
        Color.TRANSPARENT
    ), null, Shader.TileMode.CLAMP)

    init {
        val typeface = ResourcesCompat.getFont(context, R.font.montserrat_variablefont_wght)
        fillPaint.apply {
            style = Paint.Style.FILL
            this.typeface = typeface
        }
        emptyPaint.apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#4A6572")  // A darker shade for empty text
            this.typeface = typeface
        }
    }

    fun setFillProgress(progress: Float) {
        fillProgress = progress
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val text = text.toString()
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)

        val textWidth = paint.measureText(text)
        val fillWidth = textWidth * fillProgress
        val x = (width - textWidth) / 2
        val y = height / 2 + textBounds.height() / 2

        // Draw empty (unfilled) text
        emptyPaint.textSize = textSize
        canvas.drawText(text, x, y.toFloat(), emptyPaint)

        // Draw filled text
        canvas.save()
        canvas.translate(x, y.toFloat())
        val shader = Matrix()
        shader.setScale(fillWidth, 1f)
        gradient.setLocalMatrix(shader)
        fillPaint.shader = gradient
        fillPaint.textSize = textSize
        canvas.drawText(text, 0f, 0f, fillPaint)
        canvas.restore()
    }
}