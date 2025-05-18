package com.google.mediapipe.examples.gesturerecognizer.fragment

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.mediapipe.examples.gesturerecognizer.GestureRecognizerHelper
import com.google.mediapipe.examples.gesturerecognizer.MainViewModel
import com.google.mediapipe.examples.gesturerecognizer.R
import com.google.mediapipe.examples.gesturerecognizer.databinding.FragmentGalleryBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class GalleryFragment : Fragment(),
    GestureRecognizerHelper.GestureRecognizerListener {

    enum class MediaType {
        IMAGE,
        VIDEO,
        UNKNOWN
    }

    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
    private val fragmentGalleryBinding
        get() = _fragmentGalleryBinding!!
    private lateinit var gestureRecognizerHelper: GestureRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ScheduledExecutorService

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            // Handle the returned Uri
            uri?.let { mediaUri ->
                when (val mediaType = loadMediaType(mediaUri)) {
                    MediaType.IMAGE -> runGestureRecognitionOnImage(mediaUri)
                    MediaType.VIDEO -> runGestureRecognitionOnVideo(mediaUri)
                    MediaType.UNKNOWN -> {
                        updateDisplayView(mediaType)
                        Toast.makeText(
                            requireContext(),
                            "Unsupported data type.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding =
            FragmentGalleryBinding.inflate(inflater, container, false)

        return fragmentGalleryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configuration du bouton pour ouvrir la galerie
        fragmentGalleryBinding.fabGetContent.setOnClickListener {
            getContent.launch(arrayOf("image/*", "video/*"))
        }

        // Configuration du bouton pour retourner à la caméra
        fragmentGalleryBinding.btnBackToCamera.setOnClickListener {
            navigateToCamera()
        }
    }

    private fun navigateToCamera() {
        try {
            // Créer une nouvelle instance de CameraFragment
            val cameraFragment = CameraFragment()

            // Obtenir le gestionnaire de fragments à partir de l'activité
            val fragmentManager = requireActivity().supportFragmentManager

            // Effectuer la transaction de fragment avec le bon conteneur
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, cameraFragment)
                .commit()

            Toast.makeText(context, getString(R.string.returning_to_camera), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Afficher l'erreur dans les logs et à l'utilisateur
            Log.e(TAG, "Erreur lors de la navigation vers la caméra: ${e.message}", e)
            Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        fragmentGalleryBinding.overlay.clear()
        if (fragmentGalleryBinding.videoView.isPlaying) {
            fragmentGalleryBinding.videoView.stopPlayback()
        }
        fragmentGalleryBinding.videoView.visibility = View.GONE
        super.onPause()
    }

    // Load and display the image.
    private fun runGestureRecognitionOnImage(uri: Uri) {
        setUiEnabled(false)
        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        updateDisplayView(MediaType.IMAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(
                requireActivity().contentResolver,
                uri
            )
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(
                requireActivity().contentResolver,
                uri
            )
        }
            .copy(Bitmap.Config.ARGB_8888, true)
            ?.let { bitmap ->
                fragmentGalleryBinding.imageResult.setImageBitmap(bitmap)

                // Run gesture recognizer on the input image
                backgroundExecutor.execute {

                    gestureRecognizerHelper =
                        GestureRecognizerHelper(
                            context = requireContext(),
                            runningMode = RunningMode.IMAGE,
                            minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                            minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                            minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                            currentDelegate = viewModel.currentDelegate
                        )

                    gestureRecognizerHelper.recognizeImage(bitmap)
                        ?.let { resultBundle ->
                            activity?.runOnUiThread {
                                fragmentGalleryBinding.overlay.setResults(
                                    resultBundle.results[0],
                                    bitmap.height,
                                    bitmap.width,
                                    RunningMode.IMAGE
                                )

                                // This will return an empty list if there are no gestures detected
                                if(!resultBundle.results.first().gestures().isEmpty()) {
                                    val gestureName = resultBundle.results.first().gestures().first().first().categoryName()
                                    showResult(gestureName)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "لم يتم الكشف عن الأيدي",
                                        Toast.LENGTH_SHORT).show()
                                }

                                setUiEnabled(true)
                            }
                        } ?: run {
                        Log.e(
                            TAG, "Error running gesture recognizer."
                        )
                    }

                    gestureRecognizerHelper.clearGestureRecognizer()
                }
            }
    }

    // Load and display the video.
    private fun runGestureRecognitionOnVideo(uri: Uri) {
        setUiEnabled(false)
        updateDisplayView(MediaType.VIDEO)
        fragmentGalleryBinding.resultText.visibility = View.GONE

        with(fragmentGalleryBinding.videoView) {
            setVideoURI(uri)
            // mute the audio
            setOnPreparedListener { it.setVolume(0f, 0f) }
            requestFocus()
        }

        backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        backgroundExecutor.execute {

            gestureRecognizerHelper =
                GestureRecognizerHelper(
                    context = requireContext(),
                    runningMode = RunningMode.VIDEO,
                    minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                    minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                    minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                    currentDelegate = viewModel.currentDelegate
                )

            activity?.runOnUiThread {
                fragmentGalleryBinding.videoView.visibility = View.GONE
                fragmentGalleryBinding.progress.visibility = View.VISIBLE
            }

            gestureRecognizerHelper.recognizeVideoFile(uri, VIDEO_INTERVAL_MS)
                ?.let { resultBundle ->
                    activity?.runOnUiThread { displayVideoResult(resultBundle) }
                }
                ?: run {
                    activity?.runOnUiThread {
                        fragmentGalleryBinding.progress.visibility =
                            View.GONE
                    }
                    Log.e(TAG, "Error running gesture recognizer.")
                }

            gestureRecognizerHelper.clearGestureRecognizer()
        }
    }

    // Setup and display the video.
    private fun displayVideoResult(result: GestureRecognizerHelper.ResultBundle) {

        fragmentGalleryBinding.videoView.visibility = View.VISIBLE
        fragmentGalleryBinding.progress.visibility = View.GONE

        fragmentGalleryBinding.videoView.start()
        val videoStartTimeMs = SystemClock.uptimeMillis()

        backgroundExecutor.scheduleAtFixedRate(
            {
                activity?.runOnUiThread {
                    val videoElapsedTimeMs =
                        SystemClock.uptimeMillis() - videoStartTimeMs
                    val resultIndex =
                        videoElapsedTimeMs.div(VIDEO_INTERVAL_MS).toInt()

                    if (resultIndex >= result.results.size || fragmentGalleryBinding.videoView.visibility == View.GONE) {
                        // The video playback has finished so we stop drawing bounding boxes
                        setUiEnabled(true)
                        backgroundExecutor.shutdown()
                    } else {
                        fragmentGalleryBinding.overlay.setResults(
                            result.results[resultIndex],
                            result.inputImageHeight,
                            result.inputImageWidth,
                            RunningMode.VIDEO
                        )
                        val categories = result.results[resultIndex].gestures()
                        if (categories.isNotEmpty()) {
                            val gestureName = categories.first().first().categoryName()
                            showResult(gestureName)
                        } else {
                            fragmentGalleryBinding.resultText.visibility = View.GONE
                        }

                        setUiEnabled(false)
                    }
                }
            },
            0,
            VIDEO_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun showResult(gestureName: String) {
        fragmentGalleryBinding.resultText.apply {
            text = gestureName
            visibility = View.VISIBLE
        }
    }

    private fun updateDisplayView(mediaType: MediaType) {
        fragmentGalleryBinding.imageResult.visibility =
            if (mediaType == MediaType.IMAGE) View.VISIBLE else View.GONE
        fragmentGalleryBinding.videoView.visibility =
            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
        fragmentGalleryBinding.tvPlaceholder.visibility =
            if (mediaType == MediaType.UNKNOWN) View.VISIBLE else View.GONE
        fragmentGalleryBinding.resultText.visibility = View.GONE
    }

    // Check the type of media that user selected.
    private fun loadMediaType(uri: Uri): MediaType {
        val mimeType = context?.contentResolver?.getType(uri)
        mimeType?.let {
            if (mimeType.startsWith("image")) return MediaType.IMAGE
            if (mimeType.startsWith("video")) return MediaType.VIDEO
        }

        return MediaType.UNKNOWN
    }

    private fun setUiEnabled(enabled: Boolean) {
        fragmentGalleryBinding.fabGetContent.isEnabled = enabled
        fragmentGalleryBinding.btnBackToCamera.isEnabled = enabled
    }

    private fun recognitionError() {
        activity?.runOnUiThread {
            fragmentGalleryBinding.progress.visibility = View.GONE
            setUiEnabled(true)
            updateDisplayView(MediaType.UNKNOWN)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        recognitionError()
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == GestureRecognizerHelper.GPU_ERROR) {
                viewModel.setDelegate(GestureRecognizerHelper.DELEGATE_CPU)
            }
        }
    }

    override fun onResults(resultBundle: GestureRecognizerHelper.ResultBundle) {
        // no-op
    }

    companion object {
        private const val TAG = "GalleryFragment"

        // Value used to get frames at specific intervals for inference (e.g. every 300ms)
        private const val VIDEO_INTERVAL_MS = 300L
    }
}