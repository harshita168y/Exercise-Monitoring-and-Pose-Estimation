package com.google.mediapipe.examples.poselandmarker.util

import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PreExerciseChecker {

    private var precheckPassed = false

    fun check(result: PoseLandmarkerResult): String {
        if (precheckPassed) {
            return "✅ Perfect! Let's begin"
        }

        if (result.landmarks().isEmpty()) {
            return "📸 No person detected. Please stand in front of the camera."
        }

        val landmarks = result.landmarks()[0]
        val noseVisibility = landmarks[0].visibility().orElse(0.0f)
        val leftKneeVisibility = landmarks[25].visibility().orElse(0.0f)
        val rightKneeVisibility = landmarks[26].visibility().orElse(0.0f)

        if (noseVisibility < 0.5f || leftKneeVisibility < 0.5f || rightKneeVisibility < 0.5f) {
            return "🚶 Step back to fit your full body in frame."
        }

        // Once frame looks fine, mark precheck as passed
        precheckPassed = true
        return "✅ Perfect! Let's begin"
    }

    fun reset() {
        precheckPassed = false
    }
}
