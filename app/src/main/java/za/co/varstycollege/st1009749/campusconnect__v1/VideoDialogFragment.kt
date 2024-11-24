package za.co.varstycollege.st1009749.campusconnect__v1

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.VideoView
import androidx.fragment.app.DialogFragment
import android.widget.Toast
import android.util.Log

class VideoDialogFragment : DialogFragment() {
    private lateinit var videoView: VideoView
    private lateinit var progressBar: ProgressBar
    private var videoResId: Int = 0

    companion object {
        private const val ARG_VIDEO_RES_ID = "video_res_id"

        fun newInstance(videoResourceId: Int): VideoDialogFragment {
            return VideoDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_VIDEO_RES_ID, videoResourceId)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        videoResId = arguments?.getInt(ARG_VIDEO_RES_ID, 0) ?: 0
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        videoView = view.findViewById(R.id.videoView)
        progressBar = view.findViewById(R.id.progressBar)

        try {
            // Set up video path
            val videoPath = "android.resource://${requireActivity().packageName}/${videoResId}"
            videoView.setVideoURI(Uri.parse(videoPath))

            // Set up media controller
            val mediaController = MediaController(requireContext())
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)

            // Set up video view listeners
            videoView.setOnPreparedListener { mediaPlayer ->
                Log.d("VideoDialog", "Video prepared")
                progressBar.visibility = View.GONE
                mediaPlayer.start()


                mediaPlayer.setOnVideoSizeChangedListener { mp, width, height ->
                    mediaController.setAnchorView(videoView)
                }
            }

            videoView.setOnErrorListener { mp, what, extra ->
                Log.e("VideoDialog", "Error playing video: what=$what extra=$extra")
                progressBar.visibility = View.GONE
                Toast.makeText(
                    context,
                    "Error playing video",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }

            // Start loading the video
            videoView.start()

        } catch (e: Exception) {
            Log.e("VideoDialog", "Error setting up video: ${e.message}")
            progressBar.visibility = View.GONE
            Toast.makeText(
                context,
                "Error loading video",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    override fun onPause() {
        super.onPause()
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        videoView.stopPlayback()
    }
}