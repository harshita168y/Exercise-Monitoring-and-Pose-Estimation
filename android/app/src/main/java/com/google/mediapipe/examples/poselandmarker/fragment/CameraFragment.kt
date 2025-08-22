/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.poselandmarker.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Camera
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.MutableState
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.google.mediapipe.examples.poselandmarker.PoseLandmarkerHelper
import com.google.mediapipe.examples.poselandmarker.MainViewModel
import com.google.mediapipe.examples.poselandmarker.OverlayView
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentCameraBinding
import com.google.mediapipe.examples.poselandmarker.tracker.HammerCurlTracker
import com.google.mediapipe.examples.poselandmarker.viewmodel.LiveSessionViewModel
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.speech.tts.TextToSpeech
import com.google.mediapipe.examples.poselandmarker.util.PreExerciseChecker
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.examples.poselandmarker.tracker.ExerciseTracker
import com.google.mediapipe.examples.poselandmarker.tracker.FrontRaiseTracker
import com.google.mediapipe.examples.poselandmarker.tracker.WallSitTracker


private lateinit var preExerciseChecker: PreExerciseChecker
private var precheckCountdownStarted = false
private var precheckSuccessTime = 0L
private val countdownDuration = 5000L // 5 seconds

var precheckDone = false                 //
var timerStartTime: Long? = null        //
var lastPrecheckSpokenTime = 0L         //

private var countdownStarted = false
private var countdownEnded = false
private lateinit var textToSpeech: TextToSpeech
private var lastFeedback = ""
private var lastFeedbackTime = 0L




class CameraFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    companion object {
        private const val TAG = "Pose Landmarker"
        private lateinit var reps: MutableState<Int>
        private lateinit var stage: MutableState<String>
        private lateinit var feedback: MutableState<String>


        fun newInstance(): CameraFragment = CameraFragment()


    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_BACK

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService
    lateinit var previewView: PreviewView
    lateinit var overlayView: OverlayView
    lateinit var liveSessionViewModel: LiveSessionViewModel
    private lateinit var textToSpeech: TextToSpeech


    // new one
    private fun updateExerciseStatus(currentReps: Int, currentStage: String, currentFeedback: String) {
        reps.value = currentReps
        stage.value = currentStage
        feedback.value = currentFeedback
    }
    private lateinit var tracker: ExerciseTracker

    private val bicepTracker = HammerCurlTracker()


    private var sessionPhase = SessionPhase.WAITING_FOR_FRAME
    private var countdownStartTime = 0L
    private var countdownDuration = 10_000L
    private var lastFeedback = ""
    private var lastFeedbackTime = 0L



    override fun onResume() {
        super.onResume()

        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(), R.id.fragment_container
            ).navigate(R.id.action_camera_to_permissions)
        }

        // Start the PoseLandmarkerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute {
            if(this::poseLandmarkerHelper.isInitialized) {
                if (poseLandmarkerHelper.isClose()) {
                    poseLandmarkerHelper.setupPoseLandmarker()
                }
            }
        }
    }
    fun setViewModel(vm: LiveSessionViewModel) {
        liveSessionViewModel = vm
    }
    enum class SessionPhase {
        WAITING_FOR_FRAME,
        COUNTDOWN,
        EXERCISE_RUNNING
    }


    override fun onPause() {
        super.onPause()
        if(this::poseLandmarkerHelper.isInitialized) {
            viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence)
            viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence)
            viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence)
            viewModel.setDelegate(poseLandmarkerHelper.currentDelegate)

            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
    }

    override fun onDestroyView() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        _fragmentCameraBinding = null
        super.onDestroyView()


        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("CameraFragment", "onCreateView called")
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("CameraFragment", "onViewCreated called")
        previewView = fragmentCameraBinding.viewFinder
        overlayView = fragmentCameraBinding.overlay
        val selectedExercise = arguments?.getString("exercise") ?: ""
        when (selectedExercise) {
            "Hammer Curl" -> {
                tracker = HammerCurlTracker()
                Log.d("CameraFragment", "Selected Exercise: $selectedExercise")
            }

            "Wall Sitting" ->{
                tracker = WallSitTracker()
            }
            "Front Raise" ->{
                tracker = FrontRaiseTracker()
            }
            else -> {
                tracker = HammerCurlTracker()
                Log.d("CameraFragment", "Selected Exercise: $selectedExercise (defaulted to Hammer Curl)")
            }
        }


        preExerciseChecker = PreExerciseChecker()


        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech.language = Locale.US
            }
        }

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Create the PoseLandmarkerHelper that will handle the inference
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                poseLandmarkerHelperListener = this
            )
        }

        // Attach listeners to UI control widgets
        initBottomSheetControls()
    }

    private fun initBottomSheetControls() {
        // init bottom sheet settings

        fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseDetectionConfidence
            )
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPoseTrackingConfidence
            )
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.text =
            String.format(
                Locale.US, "%.2f", viewModel.currentMinPosePresenceConfidence
            )

        // When clicked, lower pose detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdMinus.setOnClickListener {
            if (poseLandmarkerHelper.minPoseDetectionConfidence >= 0.2) {
                poseLandmarkerHelper.minPoseDetectionConfidence -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise pose detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.detectionThresholdPlus.setOnClickListener {
            if (poseLandmarkerHelper.minPoseDetectionConfidence <= 0.8) {
                poseLandmarkerHelper.minPoseDetectionConfidence += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, lower pose tracking score threshold floor
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdMinus.setOnClickListener {
            if (poseLandmarkerHelper.minPoseTrackingConfidence >= 0.2) {
                poseLandmarkerHelper.minPoseTrackingConfidence -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise pose tracking score threshold floor
        fragmentCameraBinding.bottomSheetLayout.trackingThresholdPlus.setOnClickListener {
            if (poseLandmarkerHelper.minPoseTrackingConfidence <= 0.8) {
                poseLandmarkerHelper.minPoseTrackingConfidence += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, lower pose presence score threshold floor
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdMinus.setOnClickListener {
            if (poseLandmarkerHelper.minPosePresenceConfidence >= 0.2) {
                poseLandmarkerHelper.minPosePresenceConfidence -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise pose presence score threshold floor
        fragmentCameraBinding.bottomSheetLayout.presenceThresholdPlus.setOnClickListener {
            if (poseLandmarkerHelper.minPosePresenceConfidence <= 0.8) {
                poseLandmarkerHelper.minPosePresenceConfidence += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference.
        // Current options are CPU and GPU
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
            viewModel.currentDelegate, false
        )
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long
                ) {
                    try {
                        poseLandmarkerHelper.currentDelegate = p2
                        updateControlsUi()
                    } catch(e: UninitializedPropertyAccessException) {
                        Log.e(TAG, "PoseLandmarkerHelper has not been initialized yet.")
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        // When clicked, change the underlying model used for object detection
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(
            viewModel.currentModel,
            false
        )
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long
                ) {
                    poseLandmarkerHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset Poselandmarker
    // helper.
    private fun updateControlsUi() {
        if(this::poseLandmarkerHelper.isInitialized) {
            fragmentCameraBinding.bottomSheetLayout.detectionThresholdValue.text =
                String.format(
                    Locale.US,
                    "%.2f",
                    poseLandmarkerHelper.minPoseDetectionConfidence
                )
            fragmentCameraBinding.bottomSheetLayout.trackingThresholdValue.text =
                String.format(
                    Locale.US,
                    "%.2f",
                    poseLandmarkerHelper.minPoseTrackingConfidence
                )
            fragmentCameraBinding.bottomSheetLayout.presenceThresholdValue.text =
                String.format(
                    Locale.US,
                    "%.2f",
                    poseLandmarkerHelper.minPosePresenceConfidence
                )

            // Needs to be cleared instead of reinitialized because the GPU
            // delegate needs to be initialized on the thread using it when applicable
            backgroundExecutor.execute {
                poseLandmarkerHelper.clearPoseLandmarker()
                poseLandmarkerHelper.setupPoseLandmarker()
            }
            fragmentCameraBinding.overlay.clear()
        }
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectPose(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if(this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after pose have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    private val hammerCurlTracker = HammerCurlTracker()




    //
override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
    activity?.runOnUiThread {
        if (_fragmentCameraBinding != null) {
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", resultBundle.inferenceTime)

            // ✅ 1. Check if result is not empty
            if (resultBundle.results.isNotEmpty()) {
                val result = resultBundle.results.first()

                // ✅ 2. Draw pose on overlay
                fragmentCameraBinding.overlay.setResults(
                    result,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )
                fragmentCameraBinding.overlay.invalidate()

                // ✅ 3. Analyze pose and update ViewModel
                val poseLandmarks = result.landmarks().firstOrNull() ?: return@runOnUiThread
//                val trackerInput = poseLandmarks.map {
//                    com.google.mediapipe.tasks.components.containers.NormalizedLandmark.create(it.x(), it.y(), it.z())
//                }

//                val exerciseResult = bicepTracker.analyzePose(trackerInput)
//                liveSessionViewModel?.updateExerciseStatus(
//                    currentReps = exerciseResult.reps,
//                    currentStage = exerciseResult.stage,
//                    currentFeedback = exerciseResult.feedback
//                )


//                if (exerciseResult.feedback.isNotBlank()) {
//                    textToSpeech.speak(exerciseResult.feedback, TextToSpeech.QUEUE_FLUSH, null, null)
//                }
               // val currentTime = System.currentTimeMillis()
                val landmarks = result.landmarks().firstOrNull() ?: return@runOnUiThread
                val trackerInput = poseLandmarks.map {
                    NormalizedLandmark.create(it.x(), it.y(), it.z())
                }

                val currentTime = System.currentTimeMillis()

                when (sessionPhase) {
                    SessionPhase.WAITING_FOR_FRAME -> {
                        val headVisible = landmarks[0].visibility().orElse(0f)
                        val kneeVisible = landmarks[26].visibility().orElse(0f) // Right knee

                        if (headVisible > 0.5f && kneeVisible > 0.5f) {
                            if (lastFeedback != "✅ Perfect! Let's begin") {
                                textToSpeech.speak("✅ Perfect! Let's begin", TextToSpeech.QUEUE_FLUSH, null, null)
                                lastFeedback = "✅ Perfect! Let's begin"
                            }

                            countdownStartTime = currentTime
                            sessionPhase = SessionPhase.COUNTDOWN
                        } else {
                            if (lastFeedback != "Step back to fit in frame." || currentTime - lastFeedbackTime > 4000L) {
                                textToSpeech.speak("Step back to fit in frame.", TextToSpeech.QUEUE_FLUSH, null, null)
                                lastFeedback = "Step back to fit in frame."
                                lastFeedbackTime = currentTime
                            }

                            liveSessionViewModel?.updateExerciseStatus(
                                currentReps = 0,
                                currentStage = "",
                                currentFeedback = lastFeedback
                            )
                        }
                    }

                    SessionPhase.COUNTDOWN -> {

                        val elapsedTime = currentTime - countdownStartTime
                        val timeLeft = countdownDuration - (currentTime - countdownStartTime)
                        if (timeLeft > 0) {
                            val seconds = 5 - (elapsedTime / 2000).toInt()

                            // 🎯 1. Speak "Starting in 5" only at the beginning
                            val spokenMessage = when (seconds) {
                                5 -> "Starting in 5"
                                in 1..4 -> "$seconds"
                                else -> ""
                            }


                            if (spokenMessage.isNotEmpty() && spokenMessage != lastFeedback) {
                                textToSpeech.speak(spokenMessage, TextToSpeech.QUEUE_FLUSH, null, null)
                                lastFeedback = spokenMessage
                            }


                            // 🎯 2. Show complete UI message (without repeating full phrase in audio)
                            val uiMessage = if (seconds == 5) "Starting in 5..." else "$seconds..."
                            liveSessionViewModel?.updateExerciseStatus(
                                currentReps = 0,
                                currentStage = "",
                                currentFeedback = uiMessage
                            )
                        } else {
                            textToSpeech.speak("Begin!", TextToSpeech.QUEUE_FLUSH, null, null)
                            sessionPhase = SessionPhase.EXERCISE_RUNNING
                        }
                    }


                    SessionPhase.EXERCISE_RUNNING -> {
//
                        val exerciseResult = tracker.analyzePose(trackerInput)

                        liveSessionViewModel?.updateExerciseStatus(
                            currentReps = exerciseResult.reps,
                            currentStage = exerciseResult.stage,
                            currentFeedback = exerciseResult.feedback
                        )

                        // Speak only if feedback is meaningful and not repeating unnecessarily
                        if (exerciseResult.feedback.isNotEmpty() &&
                            (exerciseResult.feedback != lastFeedback || currentTime - lastFeedbackTime > 4000L)
                        ) {
                            textToSpeech.speak(exerciseResult.feedback, TextToSpeech.QUEUE_FLUSH, null, null)
                            lastFeedback = exerciseResult.feedback
                            lastFeedbackTime = currentTime
                        }
                    }
                }


            } else {
                Log.d("CameraFragment", "No pose landmarks detected")
            }
        }
    }
}

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
                    PoseLandmarkerHelper.DELEGATE_CPU, false
                )
            }
        }
    }

    private fun startCountdownWithAudio() {
        Thread {
            for (i in 5 downTo 1) {
                val msg = "⏳ Starting in $i seconds..."
                activity?.runOnUiThread {
                    liveSessionViewModel?.updateExerciseStatus(0, "", msg)
                    textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
                }
                Thread.sleep(1000)
            }
            activity?.runOnUiThread {
                countdownEnded = true
            }
        }.start()
    }
}
