package com.google.mediapipe.examples.poselandmarker.tracker

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.*

class HammerCurlTracker : ExerciseTracker {

    private var counterRight = 0
    private var counterLeft = 0
    private var stageRight: String? = null
    private var stageLeft: String? = null

    // New: Flags to prevent rep count if posture was bad
    private var badFormRight = false
    private var badFormLeft = false

    private val angleThreshold = 40  // misalignment angle between shoulder-elbow-hip
    private val angleThresholdUp = 155  // elbow flexion angle for "Flex"
    private val angleThresholdMid = 100  // mid-range to confirm "Up"
    private val angleThresholdDown = 47  // fully extended (down)

    override fun analyzePose(landmarks: List<NormalizedLandmark>): ExerciseResult {
        val shoulderRight = landmarks[12]
        val elbowRight = landmarks[14]
        val wristRight = landmarks[16]
        val hipRight = landmarks[24]

        val shoulderLeft = landmarks[11]
        val elbowLeft = landmarks[13]
        val wristLeft = landmarks[15]
        val hipLeft = landmarks[23]

        val angleRightCounter = calculateAngle(shoulderRight, elbowRight, wristRight)
        val angleLeftCounter = calculateAngle(shoulderLeft, elbowLeft, wristLeft)

        val angleRight = calculateAngle(elbowRight, shoulderRight, hipRight)
        val angleLeft = calculateAngle(elbowLeft, shoulderLeft, hipLeft)

        var feedback = ""

        // --- Right Arm ---
        if (abs(angleRight) > angleThreshold) {
            feedback = "⚠️ Fix your right arm posture"
            badFormRight = true
        }

        if (angleRightCounter > angleThresholdUp) {
            stageRight = "Flex"
        } else if (angleRightCounter >= angleThresholdMid && angleRightCounter <= angleThresholdUp) {
            stageRight = "Up"
        } else if (angleRightCounter < angleThresholdDown && (stageRight == "Up" || stageRight == "Flex")) {
            if (!badFormRight) {
                stageRight = "Down"
                counterRight++
                feedback = "Good right rep!"
            }
            // Reset after completion
            stageRight = null
            badFormRight = false
        }

        // --- Left Arm ---
        if (abs(angleLeft) > angleThreshold) {
            feedback = "⚠️ Fix your left arm posture"
            badFormLeft = true
        }

        if (angleLeftCounter > angleThresholdUp) {
            stageLeft = "Flex"
        } else if (angleLeftCounter >= angleThresholdMid && angleLeftCounter <= angleThresholdUp) {
            stageLeft = "Up"
        } else if (angleLeftCounter < angleThresholdDown && (stageLeft == "Up" || stageLeft == "Flex")) {
            if (!badFormLeft) {
                stageLeft = "Down"
                counterLeft++
                feedback = "Good left rep!"
            }
            // Reset after completion
            stageLeft = null
            badFormLeft = false
        }

        val totalReps = counterLeft + counterRight
        val stageStatus = if (stageLeft == "Flex" && stageRight == "Flex") "Up" else "Down"

        return ExerciseResult(
            reps = totalReps,
            stage = stageStatus,
            feedback = feedback
        )
    }

    private fun calculateAngle(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Double {
        val radians = atan2(c.y() - b.y(), c.x() - b.x()) -
                atan2(a.y() - b.y(), a.x() - b.x())
        var angle = abs(radians * 180.0 / Math.PI)
        if (angle > 180) angle = 360.0 - angle
        return angle
    }
}
