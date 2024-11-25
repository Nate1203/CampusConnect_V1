package za.co.varstycollege.st1009749.campusconnect__v1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class ProfileImageManager(private val context: Context) {
    companion object {
        private const val MAX_IMAGE_DIMENSION = 1024
    }

    fun updateProfileImage(
        fragment: Fragment,
        imageUri: Uri,
        profileImageView: CircleImageView,
        onSuccess: (String) -> Unit
    ) {
        try {
            // Load and compress the image
            val inputStream = context.contentResolver.openInputStream(imageUri)
            var originalBitmap = BitmapFactory.decodeStream(inputStream)

            if (originalBitmap == null) {
                Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
                return
            }

            // Get image orientation
            val rotation = getImageRotation(imageUri)

            // Rotate bitmap if needed
            if (rotation != 0f) {
                originalBitmap = rotateBitmap(originalBitmap, rotation)
            }

            // Scale down the image if it's too large
            val scaledBitmap = scaleBitmap(originalBitmap)

            // Convert to Base64
            val base64Image = bitmapToBase64(scaledBitmap)

            // Update ImageView
            profileImageView.setImageBitmap(scaledBitmap)

            // Call success callback with base64 string
            onSuccess(base64Image)

        } catch (e: Exception) {
            Log.e("ProfileImage", "Error processing image: ${e.message}")
            Toast.makeText(context, "Error updating profile picture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getImageRotation(imageUri: Uri): Float {
        try {
            val cursor = context.contentResolver.query(imageUri,
                arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
                null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val orientation = it.getInt(0)
                    return orientation.toFloat()
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileImage", "Error getting image rotation: ${e.message}")
        }
        return 0f
    }

    private fun rotateBitmap(bitmap: Bitmap, rotation: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotation)
        return Bitmap.createBitmap(bitmap, 0, 0,
            bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmap(originalBitmap: Bitmap): Bitmap {
        // If image is already smaller than maximum dimension, return original
        if (originalBitmap.width <= MAX_IMAGE_DIMENSION && originalBitmap.height <= MAX_IMAGE_DIMENSION) {
            return originalBitmap
        }

        val scale = Math.min(
            MAX_IMAGE_DIMENSION.toFloat() / originalBitmap.width,
            MAX_IMAGE_DIMENSION.toFloat() / originalBitmap.height
        )

        val scaledWidth = (originalBitmap.width * scale).toInt()
        val scaledHeight = (originalBitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()

        // Compress the bitmap to JPEG format with quality 70
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

        val imageBytes = outputStream.toByteArray()

        // Encode the byte array to Base64 string
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.e("ProfileImage", "Error converting base64 to bitmap: ${e.message}")
            null
        }
    }

    fun compressExistingImage(existingBitmap: Bitmap): String {
        val scaledBitmap = scaleBitmap(existingBitmap)
        return bitmapToBase64(scaledBitmap)
    }
}